/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014-2015  Stratehm (stratehm@hotmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with multipool-stats-backend. If not, see <http://www.gnu.org/licenses/>.
 */
package strat.mining.stratum.proxy.rest.authentication;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.utils.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.exception.NoCredentialsException;
import strat.mining.stratum.proxy.rest.ProxyResources;
import strat.mining.stratum.proxy.utils.HttpUtils;

public class AuthenticationFilter extends BaseFilter {

    private static final String WWW_AUTHENTICATE_HEADR = "WWW-Authenticate";

    private static final String API_USER = ConfigurationManager.getInstance().getApiUser();
    private static final String API_PASSWORD = ConfigurationManager.getInstance().getApiPassword();
    private static final Boolean API_READ_ONLY_ENABLED = ConfigurationManager.getInstance().getApiReadOnlyAccessEnabled();

    private String apiBasePath;
    private Set<String> publiclyAvailablePathes;

    public AuthenticationFilter(String basePath) {
        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(
                ClasspathHelper.forPackage(ProxyResources.class.getPackage().getName())).setScanners(new TypeAnnotationsScanner(),
                new MethodAnnotationsScanner()));
        apiBasePath = basePath + UriBuilder.fromResource(ProxyResources.class).build().toASCIIString();

        Set<Method> resources = reflections.getMethodsAnnotatedWith(PubliclyAvailable.class);
        publiclyAvailablePathes = new HashSet<String>();
        if (API_READ_ONLY_ENABLED) {
            for (Method resource : resources) {
                Path annotation = resource.getAnnotation(Path.class);
                if (annotation != null) {
                    publiclyAvailablePathes.add(UriBuilder.fromPath(basePath).path(resource).build().toASCIIString());
                }
            }
        }
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        NextAction nextAction = ctx.getInvokeAction();
        HttpContent httpContent = ctx.getMessage();
        final HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();
        final String requestedUri = request.getRequestURI();

        // Check the credentials only if an API_USER is set.
        if (API_USER != null) {
            if (!API_READ_ONLY_ENABLED) {
                // If read-only access is not enabled, check the credentials for
                // all requests.
                nextAction = checkCredentials(ctx, request);
            } else {
                // The request is for a static resource if it is not for an API
                // method.
                boolean isStaticResourceRequested = !requestedUri.startsWith(apiBasePath);

                // If the requested API method is publicly available, then do
                // not check the credentials.
                boolean isPubliclyAvailable = publiclyAvailablePathes.contains(requestedUri);

                // If read-only access is enabled, only check credentials for
                // non-static and non-publicly available pathes.
                if (!isStaticResourceRequested && !isPubliclyAvailable) {
                    nextAction = checkCredentials(ctx, request);
                }

            }
        }

        return nextAction;
    }

    /**
     * Check the credentials and return the next action.
     * 
     * @param ctx
     * @param request
     * @return
     */
    private NextAction checkCredentials(FilterChainContext ctx, final HttpRequestPacket request) {
        NextAction nextAction;
        try {
            Pair<String, String> credentials = HttpUtils.getCredentials(request);

            // If the the user match the API_USER and if the
            // API_PASSWORD is not set or the password math the API
            // password, then grant the access
            if (API_USER.equals(credentials.getFirst()) && (API_PASSWORD == null || API_PASSWORD.equals(credentials.getSecond()))) {
                nextAction = ctx.getInvokeAction();
            } else {
                try {
                    // If the authentication fails, wait for 1
                    // second to slow down password bruteforce
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                nextAction = sendUnauthorizedResponse(ctx, request);
            }

        } catch (NoCredentialsException e) {
            // If there is no credentials, return an unauthorized
            // response.
            nextAction = sendUnauthorizedResponse(ctx, request);
        }
        return nextAction;
    }

    /**
     * Reply immediately a 401 unauthorized response.
     * 
     * @param ctx
     */
    private NextAction sendUnauthorizedResponse(FilterChainContext ctx, HttpRequestPacket request) {
        final HttpResponsePacket response = request.getResponse();
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        response.addHeader(WWW_AUTHENTICATE_HEADR, "Basic realm=stratum-proxy");

        ctx.write(HttpContent.builder(response).content(Buffers.EMPTY_BUFFER).last(true).build());
        return ctx.getStopAction();
    }
}

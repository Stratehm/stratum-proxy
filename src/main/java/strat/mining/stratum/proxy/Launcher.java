/**
 * stratum-proxy is a proxy supporting the crypto-currency stratum pool mining
 * protocol.
 * Copyright (C) 2014  Stratehm (stratehm@hotmail.com)
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
package strat.mining.stratum.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import strat.mining.stratum.proxy.configuration.ConfigurationManager;
import strat.mining.stratum.proxy.constant.Constants;
import strat.mining.stratum.proxy.database.DatabaseManager;
import strat.mining.stratum.proxy.manager.HashrateRecorder;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.ProxyResources;
import strat.mining.stratum.proxy.rest.authentication.AuthenticationAddOn;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.worker.GetworkRequestHandler;

public class Launcher {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static Logger LOGGER = null;

	public static final String THREAD_MONITOR = "";

	private static HttpServer apiHttpServer;

	private static HttpServer getWorkHttpServer;

	public static void main(String[] args) {

		initShutdownHook();

		try {
			ConfigurationManager configurationManager = ConfigurationManager.getInstance();
			configurationManager.loadConfiguration(args);
			LOGGER = LoggerFactory.getLogger(Launcher.class);

			// Start initialization of the database manager
			initDatabaseManager();

			// Initialize the proxy manager
			initProxyManager(configurationManager);

			// Initialize the Getwork system
			initGetwork(configurationManager);

			// Initialize the rest services
			initHttpServices(configurationManager);

			// Initialize the hashrate recorder
			initHashrateRecorder();

			// Wait the end of the program
			waitInfinite();

		} catch (Exception e) {
			if (LOGGER != null) {
				LOGGER.error("Failed to start the proxy.", e);
			} else {
				System.out.println("Failed to start the proxy: ");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize the shutdown hook to close gracefully all connections.
	 */
	private static void initShutdownHook() {
		Thread hookThread = new Thread() {
			public void run() {

				// Shutdown the database
				DatabaseManager.close();

				// Start a timer task that will exit the program after 1 second
				// if the cleanup is not over.
				Timer.getInstance().schedule(new Timer.Task() {
					public void run() {
						if (LOGGER != null) {
							LOGGER.error("Force killing of the proxy...");
						} else {
							System.err.println("Force killing of the proxy...");
						}
						Runtime.getRuntime().halt(0);
					}
				}, 1000);

				if (ProxyManager.getInstance() != null) {
					if (LOGGER != null) {
						LOGGER.info("User requested shutdown... Gracefuly kill all connections...");
					} else {
						System.out.println("User requested shutdown... Gracefuly kill all connections...");
					}
					ProxyManager.getInstance().stopListeningIncomingConnections();
					ProxyManager.getInstance().closeAllWorkerConnections();
					ProxyManager.getInstance().stopPools();
				}
				if (apiHttpServer != null) {
					apiHttpServer.shutdownNow();
				}
				if (getWorkHttpServer != null) {
					getWorkHttpServer.shutdownNow();
				}
				if (LOGGER != null) {
					LOGGER.info("Shutdown !");
				} else {
					System.out.println("Shutdown !");
				}
			}
		};
		hookThread.setDaemon(true);

		Runtime.getRuntime().addShutdownHook(hookThread);
	}

	/**
	 * Initialize the hashrate recoder.
	 */
	private static void initHashrateRecorder() {
		HashrateRecorder.getInstance().startCapture();
	}

	/**
	 * Initialize the database manager.
	 */
	private static void initDatabaseManager() {
		// Just get the instance to create it.
		DatabaseManager.getInstance();
	}

	/**
	 * Initialize the HTTP services.
	 * 
	 * @param configurationManager
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private static void initHttpServices(ConfigurationManager configurationManager) throws IOException, NoSuchAlgorithmException {
		if (!ConfigurationManager.getInstance().isDisableApi()) {
			URI baseUri = UriBuilder.fromUri("https://" + configurationManager.getRestBindAddress()).port(configurationManager.getRestListenPort())
					.path("/proxy").build();
			ResourceConfig config = new ResourceConfig(ProxyResources.class);
			config.register(JacksonFeature.class);
			apiHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, false);
			ServerConfiguration serverConfiguration = apiHttpServer.getServerConfiguration();
			apiHttpServer.getListener("grizzly").getCompressionConfig().setCompressionMode(CompressionMode.ON);
			apiHttpServer.getListener("grizzly").getCompressionConfig()
					.setCompressableMimeTypes("text/javascript", "application/json", "text/html", "text/css", "text/plain");
			apiHttpServer.getListener("grizzly").getCompressionConfig().setCompressionMinSize(1024);
			HttpHandler staticHandler = getStaticHandler();
			if (staticHandler != null) {
				serverConfiguration.addHttpHandler(staticHandler, "/");
			}
			// createCertificate();

			SSLContextConfigurator sslContext = new SSLContextConfigurator();
			// sslContext.setKeyStoreFile("<path to keystore>");
			// sslContext.setKeyStorePass("<password>");
			// sslContext.setTrustStoreFile("<path to truststore>");
			// sslContext.setTrustStorePass("<password>");

			apiHttpServer.getListener("grizzly").setSecure(true);
			apiHttpServer.getListener("grizzly").setSSLEngineConfig(
					new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(true));

			apiHttpServer.getListener("grizzly").registerAddOn(new AuthenticationAddOn());

			apiHttpServer.start();
		} else {
			LOGGER.info("API port disabled. GUI will not be available.");
		}
	}

	/**
	 * Return the handler to serve static content.
	 * 
	 * @return
	 */
	private static HttpHandler getStaticHandler() {
		StaticHttpHandlerBase handler = null;
		// If the application is running form the jar file, use a Class Loader
		// to get the web content.
		if (ConfigurationManager.isRunningFromJar()) {
			try {
				File stratumProxyWebappJarFile = new File(ConfigurationManager.getInstallDirectory(), "lib/stratum-proxy-webapp.jar");
				if (stratumProxyWebappJarFile.exists()) {
					handler = new CLStaticHttpHandler(new URLClassLoader(
							new URL[] { new URL("file://" + stratumProxyWebappJarFile.getAbsolutePath()) }), "/") {

						private final Logger SUB_LOGGER = LoggerFactory.getLogger(CLStaticHttpHandler.class);

						protected boolean handle(String resourcePath, Request request, Response response) throws Exception {
							SUB_LOGGER.trace("Requested resource: {}.", resourcePath);
							long time = System.currentTimeMillis();
							String resourcePathFiltered = resourcePath;
							// If the root is requested, then replace the
							// requested resource by index.html
							if ("/".equals(resourcePath)) {
								resourcePathFiltered = "/index.html";
							}
							boolean found = super.handle(resourcePathFiltered, request, response);
							time = System.currentTimeMillis() - time;
							if (found) {
								SUB_LOGGER.trace("Resource sent in {} ms: {}.", time, resourcePath);
							} else {
								SUB_LOGGER.trace("Resource not found: {}.", resourcePath);
							}
							return found;
						}

					};
				} else {
					LOGGER.warn("lib/stratum-proxy-webapp.jar not found. GUI will not be available.");
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to initialize the Web content loader. GUI will not be available.", e);
			}
		} else {
			// If not running from a jar, it is running from the dev
			// environment. So use a static handler.
			File installPath = new File(ConfigurationManager.getInstallDirectory());
			File docRootPath = new File(installPath.getParentFile(), "src/main/resources/webapp");
			handler = new StaticHttpHandler(docRootPath.getAbsolutePath()) {
				private final Logger SUB_LOGGER = LoggerFactory.getLogger(StaticHttpHandler.class);

				protected boolean handle(String uri, Request request, Response response) throws Exception {
					SUB_LOGGER.trace("Requested resource: {}.", uri);
					long time = System.currentTimeMillis();
					boolean found = super.handle(uri, request, response);
					time = System.currentTimeMillis() - time;
					if (found) {
						SUB_LOGGER.trace("Resource sent in {} ms: {}.", time, uri);
					} else {
						SUB_LOGGER.trace("Resource not found: {}.", uri);
					}
					return found;
				}

			};
		}
		// Disable the file cache if in development.
		handler.setFileCacheEnabled(!ConfigurationManager.getVersion().equals("Dev"));

		return handler;
	}

	/**
	 * Initialize the Getwork system.
	 * 
	 * @param configurationManager
	 */
	private static void initGetwork(ConfigurationManager configurationManager) throws IOException {
		if (!ConfigurationManager.getInstance().isDisableGetwork()) {
			URI baseUri = UriBuilder.fromUri("http://" + configurationManager.getGetworkBindAddress())
					.port(configurationManager.getGetworkListenPort()).build();
			getWorkHttpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri);
			ServerConfiguration serverConfiguration = getWorkHttpServer.getServerConfiguration();
			serverConfiguration.addHttpHandler(new GetworkRequestHandler(), "/", Constants.DEFAULT_GETWORK_LONG_POLLING_URL);
		} else {
			LOGGER.info("Getwork port disabled.");
		}
	}

	/**
	 * Initialize the proxy manager
	 * 
	 * @param configurationManager
	 * @throws IOException
	 * @throws CmdLineException
	 */
	private static void initProxyManager(ConfigurationManager configurationManager) throws IOException, CmdLineException {
		List<Pool> pools = configurationManager.getPools();
		LOGGER.info("Using pools: {}.", pools);

		// Start the pools.
		ProxyManager.getInstance().startPools(pools);

		if (!ConfigurationManager.getInstance().isDisableStratum()) {
			// Start to accept incoming workers connections
			ProxyManager.getInstance().startListeningIncomingConnections(configurationManager.getStratumBindAddress(),
					configurationManager.getStratumListeningPort());
		} else {
			LOGGER.info("Stratum port disabled.");
		}
	}

	/**
	 * Wait and never return
	 */
	private static void waitInfinite() {
		try {
			synchronized (THREAD_MONITOR) {
				THREAD_MONITOR.wait();
			}
		} catch (Exception e) {
			LOGGER.info("Closing proxy...");
		}
	}

	private static void createCertificate() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
			KeyPair KPair = keyPairGenerator.generateKeyPair();
			X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
			Integer randomNumber = new SecureRandom().nextInt();
			v3CertGen.setSerialNumber(BigInteger.valueOf(randomNumber >= 0 ? randomNumber : randomNumber * -1));
			v3CertGen.setIssuerDN(new X509Principal("CN=" + "localhost" + ", OU=None, O=None L=None, C=None"));
			v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
			v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
			v3CertGen.setSubjectDN(new X509Principal("CN=" + "localhost" + ", OU=None, O=None L=None, C=None"));
			v3CertGen.setPublicKey(KPair.getPublic());
			v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");
			X509Certificate PKCertificate = v3CertGen.generateX509Certificate(KPair.getPrivate());
			// File certFile = new File("c:\\arf\\testCert.cert");
			// if (!certFile.exists()) {
			// certFile.createNewFile();
			// }
			// FileOutputStream fos = new FileOutputStream(certFile);
			// fos.write(PKCertificate.getEncoded());
			// fos.close();

			File storeFile = new File("c:\\arf\\sample-key-store.jks");
			if (!storeFile.exists()) {
				storeFile.createNewFile();
			}
			KeyStore privateKS = KeyStore.getInstance("JKS");
			privateKS.load(null, null);

			privateKS.setKeyEntry("sample.alias", KPair.getPrivate(), "charette".toCharArray(),
					new java.security.cert.Certificate[] { PKCertificate });

			privateKS.store(new FileOutputStream(storeFile), "charette".toCharArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

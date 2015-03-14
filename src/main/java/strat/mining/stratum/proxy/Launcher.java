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
package strat.mining.stratum.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
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
import strat.mining.stratum.proxy.grizzly.CLStaticHttpHandlerWithIndexSupport;
import strat.mining.stratum.proxy.grizzly.StaticHttpHandlerWithCharset;
import strat.mining.stratum.proxy.manager.HashrateRecorder;
import strat.mining.stratum.proxy.manager.ProxyManager;
import strat.mining.stratum.proxy.pool.Pool;
import strat.mining.stratum.proxy.rest.ProxyResources;
import strat.mining.stratum.proxy.rest.authentication.AuthenticationAddOn;
import strat.mining.stratum.proxy.rest.ssl.SSLRedirectAddOn;
import strat.mining.stratum.proxy.utils.Timer;
import strat.mining.stratum.proxy.worker.GetworkRequestHandler;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;

public class Launcher {

    private static final String KEYSTORE_KEY_ENTRY_ALIAS = "stratum-proxy";

    private static final String KEYSTORE_PASSWORD = "stratum-proxy";

    private static final String KEYSTORE_FILE_NAME = "stratum-proxy-keystore.jks";

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

            // Initialize the IP protocol version to use
            initIpVersion(configurationManager);

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
     * Initialize the IP protocol version to use
     * 
     * @param configurationManager
     */
    private static void initIpVersion(ConfigurationManager configurationManager) {
        if (Constants.IP_VERSION_V4.equals(configurationManager.getIpVersion())) {
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.net.preferIPv6Addresses", "false");
        } else if (Constants.IP_VERSION_V6.equals(configurationManager.getIpVersion())) {
            System.setProperty("java.net.preferIPv4Stack", "false");
            System.setProperty("java.net.preferIPv6Addresses", "true");
        } else {
            System.setProperty("java.net.preferIPv4Stack", "false");
            System.setProperty("java.net.preferIPv6Addresses", "false");
        }
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

            initializeHttpCompression();

            initializeStaticContentHandler();

            if (ConfigurationManager.getInstance().getApiEnableSsl()) {
                initializeSslEngine();
            }

            // Initialize the HTTP Basic Authentication
            apiHttpServer.getListener("grizzly").registerAddOn(new AuthenticationAddOn());

            apiHttpServer.start();
        } else {
            LOGGER.info("API port disabled. GUI will not be available.");
        }
    }

    /**
     * Initialize the handler which handles static resources HTTP requests.
     */
    private static void initializeStaticContentHandler() {
        HttpHandler staticHandler = getStaticHandler();
        if (staticHandler != null) {
            ServerConfiguration serverConfiguration = apiHttpServer.getServerConfiguration();
            serverConfiguration.addHttpHandler(staticHandler, "/");
        }
    }

    /**
     * Initialize the compression for HTTP requests.
     */
    private static void initializeHttpCompression() {
        apiHttpServer.getListener("grizzly").getCompressionConfig().setCompressionMode(CompressionMode.ON);
        apiHttpServer.getListener("grizzly").getCompressionConfig()
                .setCompressableMimeTypes("text/javascript", "application/json", "text/html", "text/css", "text/plain");
        apiHttpServer.getListener("grizzly").getCompressionConfig().setCompressionMinSize(1024);
    }

    /**
     * Initialize the SSL engine
     */
    private static void initializeSslEngine() {
        try {
            checkCertificate();
            SSLContextConfigurator sslContext = new SSLContextConfigurator();
            sslContext.setKeyStoreFile(new File(ConfigurationManager.getInstance().getDatabaseDirectory(), KEYSTORE_FILE_NAME).getAbsolutePath());
            sslContext.setKeyStorePass(KEYSTORE_PASSWORD);
            sslContext.setKeyPass(KEYSTORE_PASSWORD);

            apiHttpServer.getListener("grizzly").setSecure(true);
            apiHttpServer.getListener("grizzly").setSSLEngineConfig(
                    new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(false));

            apiHttpServer.getListener("grizzly").registerAddOn(new SSLRedirectAddOn());
        } catch (Exception e) {
            LOGGER.error("Failed to generate the HTTPS certificate. HTTP instead of HTTPS will be used.", e);
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
                handler = new CLStaticHttpHandlerWithIndexSupport(Launcher.class.getClassLoader(), "/");
            } catch (Exception e) {
                LOGGER.warn("Failed to initialize the Web content loader. GUI will not be available.", e);
            }
        } else {
            // If not running from a jar, it is running from the dev
            // environment. So use a static handler.
            File installPath = new File(ConfigurationManager.getInstallDirectory());
            File docRootPath = new File(installPath.getParentFile(), "src/main/resources/webapp");
            handler = new StaticHttpHandlerWithCharset(docRootPath.getAbsolutePath());
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

    /**
     * Check that a valid SSl certificate already exists. If not, create a new
     * one.
     * 
     * @throws Exception
     */
    private static void checkCertificate() throws Exception {
        File storeFile = new File(ConfigurationManager.getInstance().getDatabaseDirectory(), KEYSTORE_FILE_NAME);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        if (!storeFile.exists()) {
            LOGGER.info("KeyStore does not exist. Create {}", storeFile.getAbsolutePath());
            storeFile.getParentFile().mkdirs();
            storeFile.createNewFile();
            keyStore.load(null, null);

            LOGGER.info("Generating new SSL certificate.");
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            RSAKeyPairGenerator keyGenerator = new RSAKeyPairGenerator();
            keyGenerator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(101), new SecureRandom(), 2048, 14));
            AsymmetricCipherKeyPair keysPair = keyGenerator.generateKeyPair();

            RSAKeyParameters rsaPrivateKey = (RSAKeyParameters) keysPair.getPrivate();
            RSAPrivateKeySpec rsaPrivSpec = new RSAPrivateKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getExponent());
            RSAKeyParameters rsaPublicKey = (RSAKeyParameters) keysPair.getPublic();
            RSAPublicKeySpec rsaPublicSpec = new RSAPublicKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getExponent());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey rsaPriv = kf.generatePrivate(rsaPrivSpec);
            PublicKey rsaPub = kf.generatePublic(rsaPublicSpec);

            X500Name issuerDN = new X500Name("CN=localhost, OU=None, O=None, L=None, C=None");
            Integer randomNumber = new SecureRandom().nextInt();
            BigInteger serialNumber = BigInteger.valueOf(randomNumber >= 0 ? randomNumber : randomNumber * -1);
            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
            Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
            X500Name subjectDN = new X500Name("CN=localhost, OU=None, O=None, L=None, C=None");
            byte[] publickeyb = rsaPub.getEncoded();
            ASN1Sequence sequence = (ASN1Sequence) ASN1Primitive.fromByteArray(publickeyb);
            SubjectPublicKeyInfo subPubKeyInfo = new SubjectPublicKeyInfo(sequence);
            X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuerDN, serialNumber, notBefore, notAfter, subjectDN, subPubKeyInfo);

            ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(keysPair.getPrivate());
            X509CertificateHolder certificateHolder = v3CertGen.build(contentSigner);

            Certificate certificate = new CertificateFactory().engineGenerateCertificate(new ByteBufferBackedInputStream(ByteBuffer
                    .wrap(certificateHolder.toASN1Structure().getEncoded())));

            LOGGER.info("Certificate generated.");

            keyStore.setKeyEntry(KEYSTORE_KEY_ENTRY_ALIAS, rsaPriv, KEYSTORE_PASSWORD.toCharArray(),
                    new java.security.cert.Certificate[] { certificate });

            keyStore.store(new FileOutputStream(storeFile), KEYSTORE_PASSWORD.toCharArray());
        }
    }

}
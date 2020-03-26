package strat.mining.stratum.proxy.manager.proxy;

import java.util.HashMap;
import java.util.Map;

public class ProxyManagerFactory {

    private static ProxyManagerFactory instance;

    private Map<String, ProxyManagerInterface> proxies;

    private ProxyManagerFactory() {
        proxies = new HashMap<>();
        proxies.put("ProxyManager", ProxyManager.getInstance());
        proxies.put("MultiConnectProxyManager", MultiConnectProxyManager.getInstance());
    }

    public static ProxyManagerFactory getInstance() {
        if (instance == null) {
            instance = new ProxyManagerFactory();
        }
        return instance;
    }

    public ProxyManagerInterface getProxy(String name) {
        return proxies.get(name);
    }
}

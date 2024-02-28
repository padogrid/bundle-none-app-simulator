package padogrid.geode.util;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.geode.cache.client.ClientCache;

public class GeodeUtil {
    /**
     * Returns locators in string representation. An empty string if clientCache is
     * null of locators are not found.
     * 
     * @param clientCache Client cache
     */
    public final static String getLocators(ClientCache clientCache) {
        String endpoints = "";
        if (clientCache != null) {
            List<InetSocketAddress> locators = clientCache.getDefaultPool().getLocators();
            for (InetSocketAddress inetSocketAddress : locators) {
                if (endpoints.length() != 0) {
                    endpoints += ",";
                }
                endpoints = inetSocketAddress.getAddress().getHostAddress() + "[" + inetSocketAddress.getPort() + "]";
            }
        }
        return endpoints;
    }
}

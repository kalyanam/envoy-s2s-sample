package test.envoy.sds;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mkalyan on 10/28/17.
 */
public class EndpointStore {
    private Map<String, Set<Endpoint>> endpointMap = new ConcurrentHashMap<>();

    public void addEndpoint(String servicenName, Endpoint endpoint) {
        Set<Endpoint> endpoints = endpointMap.get(servicenName);
        if(endpoints == null) {
            endpoints = new HashSet<>();
            endpointMap.put(servicenName, endpoints);
        }
        endpoints.add(endpoint);
    }

    public Set<Endpoint> getEndpoints(String serviceName) {
        Set<Endpoint> endpoints = endpointMap.get(serviceName);
        if(endpoints == null) {
            return Collections.emptySet();
        } else {
            return endpoints;
        }
    }

    public Map<String, Set<Endpoint>> getAllEndpoints() {
        return Collections.unmodifiableMap(endpointMap);
    }
}

package test.envoy.sds;

/**
 * Created by mkalyan on 10/28/17.
 */
public class Endpoint {
    private String ipAddress;
    private Integer port;

    public Endpoint() {
    }

    public Endpoint(String ipAddress, Integer port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoint endpoint = (Endpoint) o;

        if (ipAddress != null ? !ipAddress.equals(endpoint.ipAddress) : endpoint.ipAddress != null) return false;
        return port != null ? port.equals(endpoint.port) : endpoint.port == null;
    }

    @Override
    public int hashCode() {
        int result = ipAddress != null ? ipAddress.hashCode() : 0;
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}

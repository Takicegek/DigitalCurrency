package network;

import java.io.Serializable;

/**
 * Created by Sorin Nutu on 2/18/2015.
 */
public class NodeInfo implements Serializable {
    private String ip;
    private int port;
    private long key; // the key of this node in the ring

    public NodeInfo(String ip, int port, long key) {
        this.ip = ip;
        this.port = port;
        this.key = key;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public long getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "key=" + key +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeInfo nodeInfo = (NodeInfo) o;

        if (key != nodeInfo.key) return false;
        if (port != nodeInfo.port) return false;
        if (!ip.equals(nodeInfo.ip)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port;
        result = 31 * result + (int) (key ^ (key >>> 32));
        return result;
    }
}

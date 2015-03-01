package network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Sorin Nutu on 2/27/2015.
 */
public class CompleteNodeInfo {
    private NodeInfo nodeInfo;
    private Streams streams;

    public CompleteNodeInfo(NodeInfo nodeInfo, Streams streams) {
        this.nodeInfo = nodeInfo;
        this.streams = streams;
    }

    public CompleteNodeInfo() {
    }

    public ObjectOutputStream getOutputStream() {
        return streams.getObjectOutputStream();
    }

    public ObjectInputStream getInputStream() {
        return streams.getObjectInputStream();
    }

    public long getKey() {
        return nodeInfo.getKey();
    }

    public String getIp() {
        return nodeInfo.getIp();
    }

    public int getPort() {
        return nodeInfo.getPort();
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public Streams getStreams() {
        return streams;
    }

    public void setStreams(Streams streams) {
        this.streams = streams;
    }

    public void closeSocket() {
        streams.closeSocket();
    }
}

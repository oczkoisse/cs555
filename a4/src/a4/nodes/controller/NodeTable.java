package a4.nodes.controller;

import java.net.InetSocketAddress;
import java.util.*;

public class NodeTable
{
    private static final int REPLICATION = 3;

    private Map<InetSocketAddress, Long> nodes;
    private Map<String, Map<Long, List<InetSocketAddress>>> replicasForChunk;

    public NodeTable()
    {
        this.nodes = new HashMap<>();
        this.replicasForChunk = new HashMap<>();
    }

    public void updateNodeInfo(InetSocketAddress listeningAddress, long freeSpace)
    {
        if (listeningAddress == null)
            throw new NullPointerException("Listening address is null");
        if (freeSpace < 0)
            throw new IllegalArgumentException("Free space cannot be negative");

        nodes.put(listeningAddress, freeSpace);
    }

    public void addReplica(String filename, long sequenceNum, InetSocketAddress listeningAddress)
    {
        if (filename == null)
            throw new NullPointerException("File name is null");

        if (listeningAddress == null)
            throw new NullPointerException("Listening address is null");

        if (sequenceNum < 0)
            throw new IllegalArgumentException("Sequence number must be non-negative");

        replicasForChunk.putIfAbsent(filename, new HashMap<>());

        replicasForChunk.get(filename).putIfAbsent(sequenceNum, new ArrayList<>());

        assert replicasForChunk.get(filename).get(sequenceNum).size() < REPLICATION;

        replicasForChunk.get(filename).get(sequenceNum).add(listeningAddress);
    }

    public List<InetSocketAddress> getReplicas(String filename, long sequenceNum)
    {
        if(replicasForChunk.containsKey(filename) && replicasForChunk.get(filename).containsKey(sequenceNum)
                && replicasForChunk.get(filename).get(sequenceNum).size() > 0)
            return Collections.unmodifiableList(replicasForChunk.get(filename).get(sequenceNum));
        return null;
    }

    public InetSocketAddress getAnyNode()
    {
        // Largest free space first
        return null;
    }

}

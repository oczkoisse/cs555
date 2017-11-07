package a4.nodes.controller;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class NodeTable
{
    private static final Random random = ThreadLocalRandom.current();

    private Map<InetSocketAddress, Long> nodes;
    private Map<String, Map<Long, Set<InetSocketAddress>>> chunkReplicas;
    private final int replication;

    public NodeTable(int replication)
    {
        this.nodes = new HashMap<>();
        this.chunkReplicas = new HashMap<>();
        this.replication = replication;
    }

    public void addNode(InetSocketAddress listeningAddress, long freeSpace)
    {
        validateAddress(listeningAddress);
        validateFreeSpace(freeSpace);

        if(nodes.containsKey(listeningAddress))
            throw new IllegalArgumentException("Cannot add already existing node: " + listeningAddress);

        nodes.put(listeningAddress, freeSpace);
    }

    public void updateNode(InetSocketAddress listeningAddress, long freeSpace)
    {
        validateAddress(listeningAddress);
        validateFreeSpace(freeSpace);

        if(!nodes.containsKey(listeningAddress))
            throw new IllegalArgumentException("Cannot update a non-existing node: " + listeningAddress);

        nodes.put(listeningAddress, freeSpace);
    }

    public void removeNode(InetSocketAddress listeningAddress)
    {
        validateAddress(listeningAddress);

        if(!nodes.containsKey(listeningAddress))
            throw new IllegalArgumentException("Cannot remove a non-existing node: " + listeningAddress);

        nodes.remove(listeningAddress);

        // Also remove that node wherever it occurs as a replica
        for(Map<Long, Set<InetSocketAddress>> map: chunkReplicas.values())
        {
            // Relies on the fact that each chunk has unique set of replicas
            for(Set<InetSocketAddress> set: map.values())
                set.remove(listeningAddress);
        }
    }

    public Set<InetSocketAddress> getAllNodes()
    {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public boolean hasNode(InetSocketAddress listeningAddress)
    {
        validateAddress(listeningAddress);

        return nodes.containsKey(listeningAddress);
    }

    public void addReplica(String filename, long sequenceNum, InetSocketAddress listeningAddress)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);
        validateAddress(listeningAddress);

        chunkReplicas.putIfAbsent(filename, new HashMap<>());

        chunkReplicas.get(filename).putIfAbsent(sequenceNum, new HashSet<>());

        if (!chunkReplicas.get(filename).get(sequenceNum).contains(listeningAddress))
            chunkReplicas.get(filename).get(sequenceNum).add(listeningAddress);
        else
            throw new IllegalArgumentException("Attempt to add duplicate replica for same chunk");
    }

    public Set<InetSocketAddress> getAllReplicas(String filename, long sequenceNum)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);

        if(chunkReplicas.containsKey(filename) && chunkReplicas.get(filename).containsKey(sequenceNum)
                && chunkReplicas.get(filename).get(sequenceNum).size() > 0)
            return Collections.unmodifiableSet(chunkReplicas.get(filename).get(sequenceNum));
        return null;
    }

    public InetSocketAddress getReplica(String filename, long sequenceNum)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);

        // For reading
        Set<InetSocketAddress> replicas = getAllReplicas(filename, sequenceNum);
        if (replicas != null)
        {
            int r = random.nextInt(replicas.size());
            int i = 0;
            for(InetSocketAddress address: replicas)
            {
                if (i == r)
                    return address;
                i++;
            }
        }
        return null;
    }

    public Set<InetSocketAddress> getCandidateReplicas(int count)
    {
        if (count <= 0)
            throw new IllegalArgumentException("Requested replica count must be a positive integer");

        Set<InetSocketAddress> nodesAddresses = nodes.keySet();

        // If not enough nodes are up yet, return null
        if (nodesAddresses.size() < count)
            return null;

        Set<InetSocketAddress> candidates = new HashSet<>();

        while (candidates.size() < count)
        {
            int n = random.nextInt(nodes.size());
            int i = 0;
            for(InetSocketAddress address: nodesAddresses)
            {
                if (i == n)
                {
                    if(!candidates.contains(address))
                        candidates.add(address);
                    break;
                }
                i++;
            }
        }
        return candidates;
    }

    private void validateFileName(String filename)
    {
        if (filename == null)
            throw new NullPointerException("File name is null");
    }

    private void validateAddress(InetSocketAddress address)
    {
        if (address == null)
            throw new NullPointerException("Address is null");
    }

    private void validateSequenceNum(long sequenceNum)
    {
        if (sequenceNum < 0)
            throw new IllegalArgumentException("Sequence number must be non-negative");
    }

    private void validateFreeSpace(long freeSpace)
    {
        if (freeSpace < 0)
            throw new IllegalArgumentException("Free space must be non-negative");
    }
}

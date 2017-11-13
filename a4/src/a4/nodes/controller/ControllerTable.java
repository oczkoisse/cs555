package a4.nodes.controller;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ControllerTable
{
    private static final Random random = ThreadLocalRandom.current();

    private final Map<InetSocketAddress, Long> nodes;
    private final Map<String, Map<Long, Set<InetSocketAddress>>> chunkReplicas;
    private final int replication;

    public ControllerTable(int replication)
    {
        this.nodes = new HashMap<>();
        this.chunkReplicas = new HashMap<>();
        this.replication = replication;
    }

    public void addNode(InetSocketAddress listeningAddress, long freeSpace)
    {
        validateAddress(listeningAddress);
        validateFreeSpace(freeSpace);

        synchronized (nodes)
        {
            if(nodes.containsKey(listeningAddress))
                throw new IllegalArgumentException("Cannot add already existing node: " + listeningAddress);
            nodes.put(listeningAddress, freeSpace);
        }
    }

    public void updateNode(InetSocketAddress listeningAddress, long freeSpace)
    {
        validateAddress(listeningAddress);
        validateFreeSpace(freeSpace);

        synchronized (nodes)
        {
            if(!nodes.containsKey(listeningAddress))
               throw new IllegalArgumentException("Cannot update a non-existing node: " + listeningAddress);

            nodes.put(listeningAddress, freeSpace);
        }
    }

    public void removeNode(InetSocketAddress listeningAddress)
    {
        validateAddress(listeningAddress);

        synchronized (nodes)
        {
            synchronized (chunkReplicas)
            {
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
        }
    }

    public Set<InetSocketAddress> getAllNodes()
    {
        synchronized (nodes)
        {
            return Collections.unmodifiableSet(nodes.keySet());
        }
    }

    public Map<InetSocketAddress, Long> getFreeSpace()
    {
        synchronized (nodes)
        {
            return Collections.unmodifiableMap(nodes);
        }
    }

    public boolean hasNode(InetSocketAddress listeningAddress)
    {
        validateAddress(listeningAddress);

        synchronized (nodes)
        {
            return nodes.containsKey(listeningAddress);
        }
    }

    public boolean addReplica(String filename, long sequenceNum, InetSocketAddress listeningAddress)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);
        validateAddress(listeningAddress);

        boolean added = false;

        synchronized (chunkReplicas)
        {
            chunkReplicas.putIfAbsent(filename, new HashMap<>());

            chunkReplicas.get(filename).putIfAbsent(sequenceNum, new HashSet<>());

            // Don't add duplicate ones
            if (!chunkReplicas.get(filename).get(sequenceNum).contains(listeningAddress)) {
                chunkReplicas.get(filename).get(sequenceNum).add(listeningAddress);
                added = true;
            }
        }
        return added;
    }

    // For recovery
    public Set<InetSocketAddress> getAllReplicas(String filename, long sequenceNum)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);

        synchronized (chunkReplicas)
        {
            if(chunkReplicas.containsKey(filename) && chunkReplicas.get(filename).containsKey(sequenceNum)
                    && chunkReplicas.get(filename).get(sequenceNum).size() > 0)
                return Collections.unmodifiableSet(chunkReplicas.get(filename).get(sequenceNum));
        }
        return null;
    }

    // For reading
    public InetSocketAddress getExistingReplica(String filename, long sequenceNum)
    {
        validateFileName(filename);
        validateSequenceNum(sequenceNum);

        // For reading
        synchronized (chunkReplicas)
        {
            Set<InetSocketAddress> replicas = getAllReplicas(filename, sequenceNum);
            if (replicas != null) {
                int r = random.nextInt(replicas.size());
                int i = 0;
                for (InetSocketAddress address : replicas) {
                    if (i == r)
                        return address;
                    i++;
                }
            }
        }
        return null;
    }

    // For writing
    public Set<InetSocketAddress> getCandidates(String filename, long sequenceNum)
    {
        synchronized (chunkReplicas)
        {
            Set<InetSocketAddress> replicas = getAllReplicas(filename, sequenceNum);
            Set<InetSocketAddress> candidates = new HashSet<>();
            if (replicas != null && replicas.size() >= replication)
                return candidates;
            else
            {
                int count = replicas == null ? replication : replication - replicas.size();
                synchronized (nodes)
                {
                    Set<InetSocketAddress> nodesAddresses = nodes.keySet();

                    // If not enough nodes are up yet, return null
                    if (nodesAddresses.size() < count)
                        return null;

                    while (candidates.size() < count)
                    {
                        int n = random.nextInt(nodes.size());
                        int i = 0;
                        for(InetSocketAddress address: nodesAddresses)
                        {
                            if (i == n)
                            {
                                if(replicas == null || !replicas.contains(address))
                                    candidates.add(address);
                                break;
                            }
                            i++;
                        }
                    }
                    return candidates;
                }
            }
        }
    }

    public void reset()
    {
        synchronized (nodes)
        {
            synchronized (chunkReplicas)
            {
                nodes.clear();
                chunkReplicas.clear();
            }
        }
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

    public Set<String> getAllFiles()
    {
        return chunkReplicas.keySet();
    }

    public Set<Long> getAllSequenceNums(String filename)
    {
        if (chunkReplicas.containsKey(filename))
            return chunkReplicas.get(filename).keySet();
        return null;
    }
}

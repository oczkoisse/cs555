package a4.nodes.server;

import a4.chunker.Metadata;

import java.util.*;

public class ServerTable {

    private Map<String, Map<Long, Metadata>> chunks;
    private List<Metadata> newChunks;

    public ServerTable() {
        this.chunks = new HashMap<>();
        this.newChunks = new ArrayList<>();
    }

    public boolean addChunk(Metadata metadata)
    {
        validateMetadata(metadata);

        boolean added = false;
        String filename = metadata.getFileName().toString();
        long sequenceNum = metadata.getSequenceNum();

        synchronized (chunks)
        {
            chunks.putIfAbsent(filename, new HashMap<>());
            Object result = chunks.get(filename).putIfAbsent(sequenceNum, metadata);
            if (result == null)
                added = true;
        }

        if (added)
            newChunks.add(metadata);

        return added;
    }

    private void validateMetadata(Metadata metadata)
    {
        if (metadata == null)
            throw new NullPointerException("Metadata is null");
    }

    public List<Metadata> getAllChunks()
    {
        List<Metadata> allChunks = new ArrayList<>();
        for(Map<Long, Metadata> map: chunks.values())
        {
            allChunks.addAll(map.values());
        }
        return allChunks;
    }

    public List<Metadata> getNewChunks()
    {
        List<Metadata> newChunksToSend = new ArrayList<>(newChunks);
        newChunks.clear();
        return Collections.unmodifiableList(newChunksToSend);
    }

    public Metadata getChunk(String filename, long seqNum)
    {
        if (chunks.containsKey(filename) && chunks.get(filename).containsKey(seqNum))
            return chunks.get(filename).get(seqNum);
        return null;
    }
}

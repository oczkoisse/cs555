package a4.nodes.server.messages;

import a4.chunker.Metadata;
import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Heartbeat extends Notification<ServerMessageType>
{
    private static final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));

    private InetSocketAddress listeningAddress;
    private List<Metadata> chunks;
    private long freeSpace;
    private int totalChunks;

    public Heartbeat(InetSocketAddress listeningAddress, List<Metadata> chunks, int totalChunks)
    {
        if (listeningAddress == null)
            throw new NullPointerException("Listening address is null");
        if (chunks == null)
            throw new NullPointerException("Chunks list is null");
        if (totalChunks < 0)
            throw new IllegalArgumentException("Total chunks must be non-negative");

        this.listeningAddress = listeningAddress;

        this.chunks = chunks;
        this.totalChunks = totalChunks;
        this.freeSpace = checkFreeSpace();
    }

    public Heartbeat()
    {
        this.listeningAddress = null;
        this.chunks = null;
        this.totalChunks = -1;
        this.freeSpace = -1;
    }

    private static long checkFreeSpace()
    {
        return tempDir.toFile().getFreeSpace();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(listeningAddress);
        out.writeLong(freeSpace);
        out.writeInt(totalChunks);
        if(this.chunks != null)
        {
            out.writeInt(chunks.size());
            for(Metadata m: chunks)
                out.writeObject(m);
        }
        else
            out.writeInt(0);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.listeningAddress = (InetSocketAddress) in.readObject();
        this.freeSpace = in.readLong();
        this.totalChunks = in.readInt();

        if (this.chunks == null)
            this.chunks = new ArrayList<>();
        else
            this.chunks.clear();

        int chunkCount = in.readInt();
        for(int i=0; i<chunkCount; i++)
            this.chunks.add((Metadata) in.readObject());
    }

    public List<Metadata> getChunksInfo()
    {
        return Collections.unmodifiableList(chunks);
    }

    public long getFreeSpace()
    {
        return freeSpace;
    }

    public int getTotalChunks()
    {
        return totalChunks;
    }

    public InetSocketAddress getListeningAddress()
    {
        return listeningAddress;
    }
}

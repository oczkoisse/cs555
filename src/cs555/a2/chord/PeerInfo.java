package cs555.a2.chord;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class PeerInfo implements Externalizable
{
    private PeerId id;
    private InetSocketAddress listeningAddress;
    private String name;

    public PeerInfo(PeerId id, InetSocketAddress listeningAddress, String name)
    {
        this.id = id;
        this.listeningAddress = listeningAddress;
        this.name = name;
    }

    public PeerInfo(PeerId id, InetSocketAddress listeningAddress)
    {
        this(id, listeningAddress, listeningAddress.getHostString());
    }

    public PeerInfo()
    {
        this.id = null;
        this.listeningAddress = null;
        this.name = null;
    }

    public PeerId getId()
    {
        if (isNotInitialized())
            throw new IllegalStateException("getId() called on PeerInfo without initializing it");
        return this.id;
    }

    public String getName()
    {
        if (isNotInitialized())
            throw new IllegalStateException("getName() called on PeerInfo without initializing it");
        return name;
    }

    public InetSocketAddress getListeningAddress()
    {
        if (isNotInitialized())
            throw new IllegalStateException("getListeningAddress() called on PeerInfo without initializing it");
        // InetSocketAddress is immutable, so this is safe
        return listeningAddress;
    }

    private boolean isNotInitialized()
    {
        return this.id == null || this.listeningAddress == null || this.name == null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (isNotInitialized())
            throw new IllegalStateException("Attempt to write PeerInfo without initializing it");
        out.writeObject(this.id);
        out.writeUTF(this.listeningAddress.getHostString());
        out.write(this.listeningAddress.getPort());
        out.writeUTF(this.name);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.id = (PeerId) in.readObject();
        String hostString = in.readUTF();
        int port = in.readInt();
        this.listeningAddress = new InetSocketAddress(hostString, port);
        this.name = in.readUTF();
    }
}

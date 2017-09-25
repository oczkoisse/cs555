package cs555.a2.chord.peer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

/**
 * PeerInfo encapsulates information about a peer that is relevant to Chord
 * i.e. the identifier, the address at which peer is listening, and an optional name
 * PeerInfo is immutable, so it is safe to share.
 */
public class PeerInfo implements Externalizable
{
    private ID id;
    private InetSocketAddress listeningAddress;
    private String name;

    public PeerInfo(ID id, InetSocketAddress listeningAddress, String name)
    {
        this.id = id;
        this.listeningAddress = listeningAddress;
        this.name = name;
    }

    public PeerInfo(ID id, InetSocketAddress listeningAddress)
    {
        this(id, listeningAddress, listeningAddress.getHostString());
    }

    public PeerInfo()
    {
        this.id = null;
        this.listeningAddress = null;
        this.name = null;
    }

    public ID getID()
    {
        if (isNotInitialized())
            throw new IllegalStateException("get() called on PeerInfo without initializing it");
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
        this.id = (ID) in.readObject();
        String hostString = in.readUTF();
        int port = in.readInt();
        this.listeningAddress = new InetSocketAddress(hostString, port);
        this.name = in.readUTF();
    }
}

package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.ID;
import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LookupRequest implements Message<ChordMessageType>
{
    private ID idToBeLookedUp;
    private int hopCount;
    private PeerInfo source;
    private LookupCause cause;
    private List<ID> path;

    public LookupRequest()
    {
        this.idToBeLookedUp = null;
        this.hopCount = 0;
        this.source = PeerInfo.NULL_PEER;
        this.cause = null;
        this.path = new ArrayList<>();
    }

    public LookupRequest(ID id, PeerInfo source, LookupCause cause)
    {
        this.idToBeLookedUp = id;
        this.hopCount = 0;
        this.source = source;
        this.cause = cause;
        this.path = new ArrayList<>();
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LOOKUP_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.source == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with source set to NULL_PEER");
        if (this.idToBeLookedUp == null)
            throw new IllegalStateException("Attempt to write with ID set to null");
        if (this.cause == null)
            throw new IllegalStateException("Attempt to write with cause set to null");
        if (this.path == null)
            throw new IllegalStateException("Attempt to write with path set to null");

        out.writeObject(idToBeLookedUp);
        out.writeInt(hopCount);
        out.writeObject(source);
        out.writeObject(cause);

        out.writeInt(path.size());
        for(ID i: path)
            out.writeObject(i);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.idToBeLookedUp = (ID) in.readObject();
        this.hopCount = in.readInt();
        this.hopCount++;
        this.source = (PeerInfo) in.readObject();
        this.cause = (LookupCause) in.readObject();

        int pathLength = in.readInt();
        for(int i=0; i<pathLength; i++)
            path.add((ID) in.readObject());
    }

    public ID getID()
    {
        return idToBeLookedUp;
    }

    public int getHopCount()
    {
        return hopCount;
    }

    public PeerInfo getSource()
    {
        return source;
    }

    public LookupCause getCause()
    {
        return cause;
    }

    @Override
    public String toString()
    {
        return getMessageType() + ": Hop " + hopCount + ", ID: " + idToBeLookedUp +
                ", Source: " + source;
    }

    public List<ID> getPath()
    {
        return Collections.unmodifiableList(path);
    }

    public void addPath(ID id)
    {
        if (id != null)
            path.add(id);
    }
}

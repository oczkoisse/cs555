package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.ID;
import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LookupRequest implements Message<ChordMessageType>
{
    private ID idToBeLookedUp;
    private int hopCount;
    private PeerInfo source;
    private boolean causedByJoin;

    public LookupRequest()
    {
        this.idToBeLookedUp = null;
        this.hopCount = 0;
        this.source = null;
        this.causedByJoin = false;
    }

    public LookupRequest(ID id, PeerInfo source, boolean causedByJoin)
    {
        this.idToBeLookedUp = id;
        this.hopCount = 0;
        this.source = source;
        this.causedByJoin = causedByJoin;
    }

    public LookupRequest(ID id, PeerInfo source)
    {
        this(id, source, false);
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LOOKUP_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(idToBeLookedUp);
        out.writeInt(hopCount);
        out.writeObject(source);
        out.writeBoolean(causedByJoin);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.idToBeLookedUp = (ID) in.readObject();
        this.hopCount = in.readInt();
        this.hopCount++;
        this.source = (PeerInfo) in.readObject();
        this.causedByJoin = in.readBoolean();
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

    public boolean isCausedByJoin()
    {
        return causedByJoin;
    }
}

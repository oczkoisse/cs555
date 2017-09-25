package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.ID;
import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LookupResult implements Message<ChordMessageType>
{
    private PeerInfo successor;
    private ID lookedUpID;
    private boolean causedByJoin;

    public LookupResult(PeerInfo successor, LookupRequest request)
    {
        this.successor = successor;
        this.lookedUpID = request.getID();
        this.causedByJoin = request.isCausedByJoin();
    }

    public LookupResult()
    {
        this.successor = null;
        this.lookedUpID = null;
        this.causedByJoin = false;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LOOKUP_RESULT;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(successor);
        out.writeObject(lookedUpID);
        out.writeBoolean(causedByJoin);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.successor = (PeerInfo) in.readObject();
        this.lookedUpID = (ID) in.readObject();
        this.causedByJoin = in.readBoolean();
    }

    public PeerInfo getSuccessor()
    {
        return successor;
    }

    public ID getLookedUpID()
    {
        return lookedUpID;
    }

    public boolean isCausedByJoin()
    {
        return causedByJoin;
    }
}

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
    private LookupCause cause;

    public LookupResult(PeerInfo successor, LookupRequest request)
    {
        this.successor = successor;
        this.lookedUpID = request.getID();
        this.cause = request.getCause();
    }

    public LookupResult()
    {
        this.successor = PeerInfo.NULL_PEER;
        this.lookedUpID = null;
        this.cause = null;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LOOKUP_RESULT;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.successor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with successor set to NULL_PEER");
        if (this.lookedUpID == null)
            throw new IllegalStateException("Attempt to write with ID set to null");
        if (this.cause == null)
            throw new IllegalStateException("Attempt to write with cause set to null");
        out.writeObject(successor);
        out.writeObject(lookedUpID);
        out.writeObject(cause);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.successor = (PeerInfo) in.readObject();
        this.lookedUpID = (ID) in.readObject();
        this.cause = (LookupCause) in.readObject();
    }

    public PeerInfo getSuccessor()
    {
        return successor;
    }

    public ID getLookedUpID()
    {
        return lookedUpID;
    }

    public LookupCause getCause()
    {
        return cause;
    }
}

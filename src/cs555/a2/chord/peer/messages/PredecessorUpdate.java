package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PredecessorUpdate implements Message<ChordMessageType>
{
    private PeerInfo latestPredecessor;

    public PredecessorUpdate()
    {
        this.latestPredecessor = null;
    }

    public PredecessorUpdate(PeerInfo predecessor)
    {
        this.latestPredecessor = predecessor;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.PRED_UPDATE;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.latestPredecessor == null)
            throw new IllegalStateException("");
        out.writeObject(this.latestPredecessor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.latestPredecessor = (PeerInfo) in.readObject();
    }

    public PeerInfo getLatestPredecessor()
    {
        return this.latestPredecessor;
    }
}
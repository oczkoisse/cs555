package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PredecessorResult implements Message<ChordMessageType>
{
    private PeerInfo predecessor;

    public PredecessorResult(PeerInfo predecessor)
    {
        this.predecessor = predecessor;
    }

    public PredecessorResult()
    {
        this.predecessor = null;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.PRED_RESULT;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(predecessor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.predecessor = (PeerInfo) in.readObject();
    }
}

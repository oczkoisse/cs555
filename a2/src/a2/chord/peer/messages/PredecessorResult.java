package a2.chord.peer.messages;

import a2.chord.peer.PeerInfo;
import a2.transport.Message;

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
        this.predecessor = PeerInfo.NULL_PEER;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.PRED_RESULT;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.predecessor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with predecessor set to NULL_PEER");
        out.writeObject(predecessor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.predecessor = (PeerInfo) in.readObject();
    }

    public PeerInfo getPredecessor()
    {
        return predecessor;
    }
}

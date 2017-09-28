package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LastGaspPredecessor implements Message<ChordMessageType>
{
    private PeerInfo predecessor;

    public LastGaspPredecessor()
    {
        this.predecessor = PeerInfo.NULL_PEER;
    }

    public LastGaspPredecessor(PeerInfo pred)
    {
        this.predecessor = pred;
    }


    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LAST_GASP_PRED;
    }


    public PeerInfo getPredecessor()
    {
        return predecessor;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.predecessor == null || this.predecessor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with predecessor set to NULL_PEER");

        out.writeObject(predecessor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.predecessor = (PeerInfo) in.readObject();
    }
}

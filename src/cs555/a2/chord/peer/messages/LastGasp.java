package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LastGasp implements Message<ChordMessageType>
{
    private PeerInfo predecessor;
    private PeerInfo successor;

    public LastGasp()
    {
        this.predecessor = PeerInfo.NULL_PEER;
        this.successor = PeerInfo.NULL_PEER;
    }

    public LastGasp(PeerInfo pred, PeerInfo succ)
    {
        this.predecessor = pred;
        this.successor = succ;
    }


    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LAST_GASP;
    }

    public PeerInfo getSuccessor()
    {
        return successor;
    }

    public PeerInfo getPredecessor()
    {
        return predecessor;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.successor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with successor set to NULL_PEER");
        if (this.predecessor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with predecessor set to NULL_PEER");

        out.writeObject(predecessor);
        out.writeObject(successor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.predecessor = (PeerInfo) in.readObject();
        this.successor = (PeerInfo) in.readObject();
    }
}

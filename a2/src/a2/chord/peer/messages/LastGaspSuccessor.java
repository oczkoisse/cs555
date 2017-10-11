package a2.chord.peer.messages;

import a2.chord.peer.PeerInfo;
import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class LastGaspSuccessor implements Message<ChordMessageType>
{
    private PeerInfo successor;

    public LastGaspSuccessor()
    {
        this.successor = PeerInfo.NULL_PEER;
    }

    public LastGaspSuccessor(PeerInfo succ)
    {
        this.successor = succ;
    }


    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.LAST_GASP_SUCC;
    }


    public PeerInfo getSuccessor()
    {
        return successor;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.successor == null || this.successor == PeerInfo.NULL_PEER)
            throw new IllegalStateException("Attempt to write with successor set to NULL_PEER");

        out.writeObject(successor);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.successor = (PeerInfo) in.readObject();
    }

}
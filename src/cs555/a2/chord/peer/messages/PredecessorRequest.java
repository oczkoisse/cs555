package cs555.a2.chord.peer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

// Should only be sent to successor
public class PredecessorRequest implements Message<ChordMessageType>
{
    private PeerInfo source;

    public PredecessorRequest() {
        this.source = null;
    }

    public PredecessorRequest(PeerInfo source)
    {
        this.source = source;
    }

    @Override
    public ChordMessageType getMessageType()
    {
        return ChordMessageType.PRED_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(source);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.source = (PeerInfo) in.readObject();
    }

    public PeerInfo getSource() {
        return source;
    }
}

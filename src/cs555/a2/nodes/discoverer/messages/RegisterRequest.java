package cs555.a2.nodes.discoverer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RegisterRequest implements Message<DiscovererMessageType>
{
    private PeerInfo source;

    public RegisterRequest()
    {
        this.source = null;
    }

    public RegisterRequest(PeerInfo source)
    {
        this.source = source;
    }

    @Override
    public DiscovererMessageType getMessageType()
    {
        return DiscovererMessageType.REGISTER_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.source == null)
            throw new IllegalStateException("Attempt to write RegisterRequest without initializing it");
        out.writeObject(source);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.source = (PeerInfo) in.readObject();
    }

    public PeerInfo getSource()
    {
        return source;
    }
}

package a2.nodes.discoverer.messages;

import a2.chord.peer.PeerInfo;
import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PeerRequest implements Message<DiscovererMessageType>
{
    private PeerInfo source;

    public PeerRequest()
    {
        this.source = null;
    }

    public PeerRequest(PeerInfo source)
    {
        this.source = source;
    }

    @Override
    public DiscovererMessageType getMessageType()
    {
        return DiscovererMessageType.PEER_REQUEST;
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

    public PeerInfo getSource()
    {
        return source;
    }
}

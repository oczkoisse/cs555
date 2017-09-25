package cs555.a2.nodes.discoverer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PeerRequest implements Message<DiscoverMessageType>
{
    private PeerInfo source;

    public PeerRequest()
    {
        this.source = null;
    }

    public PeerRequest(PeerInfo source, PeerInfo peer)
    {
        this.source = source;
    }

    @Override
    public DiscoverMessageType getMessageType()
    {
        return DiscoverMessageType.PEER_REQUEST;
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

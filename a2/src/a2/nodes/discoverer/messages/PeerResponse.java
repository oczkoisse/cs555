package a2.nodes.discoverer.messages;

import a2.chord.peer.PeerInfo;
import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PeerResponse implements Message<DiscovererMessageType>
{
    PeerInfo peer;

    public PeerResponse()
    {
        this.peer = null;
    }

    public PeerResponse(PeerInfo peer)
    {
        this.peer = peer;
    }

    @Override
    public DiscovererMessageType getMessageType()
    {
        return DiscovererMessageType.PEER_RESPONSE;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(peer);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.peer = (PeerInfo) in.readObject();
    }

    public PeerInfo getPeer()
    {
        return peer;
    }
}

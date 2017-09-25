package cs555.a2.nodes.discoverer.messages;

import cs555.a2.chord.peer.PeerInfo;
import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PeerResponse implements Message<DiscoverMessageType>
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
    public DiscoverMessageType getMessageType()
    {
        return DiscoverMessageType.PEER_RESPONSE;
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
}

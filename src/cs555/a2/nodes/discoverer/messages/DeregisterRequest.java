package cs555.a2.nodes.discoverer.messages;

import cs555.a2.chord.PeerInfo;
import cs555.a2.transport.messenger.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DeregisterRequest implements Message<DiscoverMessageType>
{
    private PeerInfo peerInfo;

    public DeregisterRequest()
    {
        this.peerInfo = null;
    }

    @Override
    public DiscoverMessageType getMessageType()
    {
        return DiscoverMessageType.DEREGISTER_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.peerInfo == null)
            throw new IllegalStateException("Attempt to write RegisterRequest without initializing it");
        out.writeObject(peerInfo);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.peerInfo = (PeerInfo) in.readObject();
    }

    public PeerInfo getPeerInfo()
    {
        return peerInfo;
    }
}

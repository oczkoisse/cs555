package a2.nodes.discoverer.messages;

import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class DeregisterResponse implements Message<DiscovererMessageType>
{
    private boolean success;

    public DeregisterResponse()
    {
        this(false);
    }

    public DeregisterResponse(boolean success)
    {
        this.success = success;
    }

    @Override
    public DiscovererMessageType getMessageType()
    {
        return DiscovererMessageType.DEREGISTER_RESPONSE;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeBoolean(success);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.success = in.readBoolean();
    }

    public boolean getStatus()
    {
        return success;
    }
}

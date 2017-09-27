package cs555.a2.nodes.discoverer.messages;

import cs555.a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RegisterResponse implements Message<DiscovererMessageType>
{
    private boolean success;

    public RegisterResponse()
    {
        this(false);
    }

    public RegisterResponse(boolean success)
    {
        this.success = success;
    }

    @Override
    public DiscovererMessageType getMessageType()
    {
        return DiscovererMessageType.REGISTER_RESPONSE;
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

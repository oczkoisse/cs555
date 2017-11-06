package a4.nodes.controller.messages;

import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MajorHeartbeatRequest implements Message<ControllerMessageType>
{
    @Override
    public ControllerMessageType getMessageType()
    {
        return ControllerMessageType.MAJOR_HEARTBEAT_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {

    }
}

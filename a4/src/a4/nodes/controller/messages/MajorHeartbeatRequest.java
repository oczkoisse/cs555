package a4.nodes.controller.messages;

import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MajorHeartbeatRequest extends Notification<ControllerMessageType>
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

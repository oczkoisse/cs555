package a4.nodes.controller.messages;

import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CheckIfAlive implements Message<ControllerMessageType>
{
    @Override
    public ControllerMessageType getMessageType()
    {
        return ControllerMessageType.CHECK_IF_ALIVE;
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

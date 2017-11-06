package a4.nodes.server.messages;

import a2.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CheckIfAlive implements Message<ServerMessageType>
{
    @Override
    public ServerMessageType getMessageType()
    {
        return ServerMessageType.CHECK_IF_ALIVE;
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

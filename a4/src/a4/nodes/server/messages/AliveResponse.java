package a4.nodes.server.messages;

import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class AliveResponse extends Notification<ServerMessageType>
{
    public AliveResponse()
    {
    }

    @Override
    public ServerMessageType getMessageType()
    {
        return ServerMessageType.ALIVE_RESPONSE;
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

package cs555.a2.transport.messenger;

import java.net.Socket;

public class ConnectionReceived extends Event
{
    private Socket sock = null;

    @Override
    public EventType getEventType()
    {
        return EventType.CONNECTION_RECEIVED;
    }

    public Socket getSocket()
    {
        return sock;
    }

    public boolean setSocket(Socket sock)
    {
        if (sock != null && this.sock == null) {
            this.sock = sock;
            return true;
        }
        return false;
    }
}

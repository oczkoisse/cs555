package a4.transport.messenger;

import java.net.Socket;

/**
 * An {@link Event} representing that a new connection has been received by {@link Messenger}
 */
public class ConnectionReceived extends Event
{
    private Socket sock = null;

    /**
     * Returns the event type
     * @return {@link EventType#CONNECTION_RECEIVED}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.CONNECTION_RECEIVED;
    }

    /**
     * Retrieve the {@link Socket} representing the received connection
     * @return {@link Socket} representing the received connection
     */
    public Socket getSocket()
    {
        return sock;
    }

    /**
     * Set the {@link Socket} information in the event
     * @param sock the {@link Socket} representing the new connection
     * @return {@code true} if {@code sock} is not null,  and no {@link Socket} was set previously, else {@code false}
     */
    boolean setSocket(Socket sock)
    {
        if (sock != null && this.sock == null) {
            this.sock = sock;
            return true;
        }
        return false;
    }
}

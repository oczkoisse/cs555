package cs555.a2.transport.messenger;

import cs555.a2.transport.Message;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * An {@link Event} representing that a new message has been received by {@link Messenger}
 */
public class MessageReceived extends Event
{
    private Message msg = null;
    private InetSocketAddress source = null;

    MessageReceived(InetSocketAddress source)
    {
        if (source == null)
            throw new IllegalArgumentException("Passed source address can't be null in a MessageReceived event");
        this.source = source;
    }

    MessageReceived(Socket sock)
    {
        if (sock == null)
            throw new IllegalArgumentException("Passed socket can't be null in a MessageReceived event");
        this.source = new InetSocketAddress(sock.getInetAddress().getHostAddress(), sock.getPort());
    }

    /**
     * Returns the event type
     * @return {@link EventType#MESSAGE_RECEIVED}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_RECEIVED;
    }

    boolean setMessage(Message msg)
    {
        if (msg == null)
            throw new IllegalArgumentException("null Message passed to setMessage()");
        if (this.msg == null)
        {
            this.msg = msg;
            return true;
        }
        return false;
    }

    /**
     * Retrives the received message
     * @return {@link Message} representing the received message
     */
    public Message getMessage()
    {
        if (this.msg == null)
            throw new IllegalStateException("getMessage() called on a MessageReceived event with no set message");
        return msg;
    }

    public InetSocketAddress getSource()
    {
        return source;
    }
}

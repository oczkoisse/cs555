package a4.transport.messenger;

import a4.transport.Message;

import java.net.InetSocketAddress;

/**
 * An {@link Event} representing that a message has been sent by {@link Messenger}
 */
public class MessageSent extends Event
{
    private Message msg = null;
    private InetSocketAddress destination = null;

    MessageSent(Message msg, InetSocketAddress destination)
    {
        if (msg == null || destination == null)
            throw new IllegalArgumentException("Message and destination can't be null in MessageSent event");
        this.msg = msg;
        this.destination = destination;
    }

    /**
     * Retrives the sent message
     * @return {@link Message} representing the sent message
     */
    public Message getMessage()
    {
        return msg;
    }

    public InetSocketAddress getDestination()
    {
        return destination;
    }

    /**
     * Returns the event type
     * @return {@link EventType#MESSAGE_SENT}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_SENT;
    }

}

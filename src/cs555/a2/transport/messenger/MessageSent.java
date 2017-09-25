package cs555.a2.transport.messenger;

/**
 * An {@link Event} representing that a message has been sent by {@link Messenger}
 */
public class MessageSent extends Event
{
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

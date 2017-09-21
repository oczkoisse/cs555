package cs555.a2.transport.messenger;

/**
 * An {@link Event} representing that a new message has been received by {@link Messenger}
 */
public class MessageReceived extends Event
{
    private Message msg = null;

    /**
     * Returns the event type
     * @return {@link EventType#MESSAGE_RECEIVED}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.MESSAGE_RECEIVED;
    }

    /**
     * Retrives the received message
     * @return {@link Message} representing the received message
     */
    public Message getMessage()
    {
        return msg;
    }

    /**
     * Set the {@link Message} information in the event
     * @param msg the {@link Message} representing the received message
     * @return {@code true} if {@code msg} is not null,  and no {@link Message} was set previously, else {@code false}
     */
    boolean setMessage(Message msg)
    {
        if (msg != null && this.msg == null) {
            this.msg = msg;
            return true;
        }
        return false;
    }
}

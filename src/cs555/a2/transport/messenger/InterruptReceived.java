package cs555.a2.transport.messenger;

/**
 * An {@link Event} representing that one of the tasks in {@link Messenger} was interrupted while waiting to complete
 */
public class InterruptReceived extends Event
{
    /**
     * Returns the event type
     * @return {@link EventType#INTERRUPT_RECEIVED}
     */
    @Override
    public EventType getEventType()
    {
        return EventType.INTERRUPT_RECEIVED;
    }
}

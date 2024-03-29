package a2.transport.messenger;

/**
 * Base class representing the events generated by {@link Messenger}
 */
public abstract class Event
{
    private Exception ex = null;

    /**
     * Returns the underlying enumeration type of this event
     * @return the underlying enumeration type of this event
     */
    public abstract EventType getEventType();

    /**
     * Returns {@code true} if there was some problem while generating the event, else {@code false}.
     * Use this before using the underlying information encapsulated in the event. If {@code true},
     * use {@link #getException()} to retrieve the excpetion which caused the event to fail.
     * @return {@code true} if there was some problem while generating the event, else {@code false}
     */
    public final boolean causedException() {
        return ex != null;
    }

    /**
     * Retrieve the {@code Exception} that caused the event to fail
     * @return {@code Exception} instance if there was an exception, else {@code null}
     */
    public final Exception getException()
    {
        return ex;
    }

    /**
     * Sets the {@code Exception} information in the event that caused it to fail
     * @return {@code true} if {@code ex} is not null, and no {@code Exception} was set previously, else {@code false}
     */
    final boolean setException(Exception ex)
    {
        if (ex != null && this.ex == null)
        {
            this.ex = ex;
            return true;
        }
        return false;
    }
}
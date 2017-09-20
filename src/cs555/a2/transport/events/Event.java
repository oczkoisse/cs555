package cs555.a2.transport.events;

public abstract class Event
{
    Exception e = null;

    public abstract EventType getEventType();

    public boolean isSuccessfull() {
        return e != null;
    }

    public final Exception getException()
    {
        return e;
    }

    public final boolean setException(Exception e)
    {
        if (e != null && this.e != null)
        {
            this.e = e;
            return true;
        }
        return false;
    }
}
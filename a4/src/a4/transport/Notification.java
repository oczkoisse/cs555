package a4.transport;

public abstract class Notification<E extends Enum<E>> implements Message<E> {

    @Override
    public final boolean isSynchronous()
    {
        return false;
    }
}

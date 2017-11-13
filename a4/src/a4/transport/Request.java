package a4.transport;

public abstract class Request<E extends Enum<E>> implements Message<E> {

    @Override
    public final boolean isSynchronous()
    {
        return true;
    }
}

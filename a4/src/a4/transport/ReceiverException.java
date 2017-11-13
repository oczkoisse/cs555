package a4.transport;

public class ReceiverException extends Exception {

    public ReceiverException(String message)
    {
        super(message);
    }

    public ReceiverException(Throwable cause)
    {
        super(cause);
    }

    public ReceiverException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

package a4.transport;

public class SenderException extends Exception {
    public SenderException(String message)
    {
        super(message);
    }

    public SenderException(Throwable cause)
    {
        super(cause);
    }

    public SenderException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

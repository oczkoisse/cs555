package a4.transport.messenger;

import a4.transport.Request;
import java.net.Socket;

public class RequestReceived extends MessageReceived {

    private Socket sock = null;

    RequestReceived(Socket sock)
    {
        if (sock == null)
            throw new NullPointerException("Socket is null");
        this.sock = sock;
    }


    boolean setRequest(Request request)
    {
        return super.setMessage(request);
    }

    /**
     * Retrieves the received request
     * @return {@link Request} representing the received request
     */
    public Request getRequest()
    {
        return (Request) super.getMessage();
    }

    public Socket getSource()
    {
        return sock;
    }

    @Override
    public EventType getEventType() {
        return EventType.REQUEST_RECEIVED;
    }
}

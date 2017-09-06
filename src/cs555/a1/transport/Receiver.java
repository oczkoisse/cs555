package cs555.a1.transport;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import cs555.a1.messages.Message;

public abstract class Receiver {

    private static final Logger LOGGER = Logger.getLogger(Receiver.class.getName());

    private Socket sock;
    private ObjectInputStream ins;

    public Receiver(Socket sock)
    {
        this.sock = sock;
        try
        {
            this.ins = new ObjectInputStream(sock.getInputStream());
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new IllegalStateException("Cannot get input stream from socket");
        }
    }

    public Message receive()
    {
        try
        {
            Message m = (Message) ins.readObject();
            return m;
        }
        catch (ClassNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new IllegalStateException("Invalid Message received");
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new IllegalStateException("Cannot read the message");
        }
    }

    public void close()
    {
        try
        {
            ins.close();
            sock.close();
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            throw new IllegalStateException("Unable to close");
        }
    }

    public abstract void handleMessage(Message m, InetAddress addr);
}

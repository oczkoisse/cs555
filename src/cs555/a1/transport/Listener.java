package cs555.a1.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Listener implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Listener.class.getName());
	
	protected ServerSocket sock;
	
	public Listener(int port, boolean reuse)
	{
		try
		{
			sock = new ServerSocket(port);
			if (reuse)
				sock.setReuseAddress(reuse);
		}
		catch(IllegalArgumentException | IOException e)
		{
			LOGGER.log(Level.SEVERE, e.toString(), e);
			throw new IllegalStateException("Server socket failed to initialize");
		}
	}

	public Listener(int port)
	{
		this(port, false);
	}
	
	public Listener()
	{
		this(0);
	}
	
	public abstract void handleClient(Socket s);
    protected boolean closeOther()
    {
        return true;
    }

	public boolean close()
	{
	    boolean closed = false;
	    try
        {
            sock.close();
            closed = true;
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        closed &= closeOther();
	    return closed;
	}

	public void run()
	{
		try
		{
			while (true)
			{
				Socket s = sock.accept();
				handleClient(s);
			}
		}
		catch(IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
        finally
        {
            if (!close())
                LOGGER.log(Level.WARNING, "Unable to close the underlying socket");
        }
	}
}

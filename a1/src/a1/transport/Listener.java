package a1.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Listener implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Listener.class.getName());
	
	protected ServerSocket sock;
	private AtomicBoolean closed = new AtomicBoolean(false);

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
	    if (this.closed.get())
	        return true;

	    boolean success = false;
	    try
        {
            sock.close();
            success = true;
        }
        catch(IOException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        success &= closeOther();
	    if (success)
        {
            this.closed.set(true);
        }
	    return success;
	}

	public void run()
	{
		try
		{
			while (!closed.get())
			{
				Socket s = sock.accept();
				handleClient(s);
			}
		}
		catch (SocketException e)
        {
            LOGGER.log(Level.INFO, "Remote host disconnected");
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

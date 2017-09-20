package cs555.a2.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Listener  {

	private static final Logger LOGGER = Logger.getLogger(Listener.class.getName());

	private ServerSocket sock;

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

	public Socket accept() throws IOException
    {
        return sock.accept();
    }

    public void close() throws IOException
    {
        if (!sock.isClosed())
            sock.close();
    }

    public boolean isClosed()
    {
        return sock.isClosed();
    }
}

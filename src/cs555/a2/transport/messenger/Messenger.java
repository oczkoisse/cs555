package cs555.a2.transport.messenger;

import cs555.a2.transport.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Messenger
{
    private static final Logger LOGGER = Logger.getLogger(Messenger.class.getName());

    private ExecutorService executorService;
    private ExecutorCompletionService<Event> executorCompletionService;
    private Listener listener;

    public Messenger(int listeningPort, int nThreadsForMessaging)
    {
        // 1 extra for listening
        this.executorService = Executors.newFixedThreadPool(nThreadsForMessaging + 1);
        this.executorCompletionService = new ExecutorCompletionService<>(executorService);
        this.listener = new Listener(listeningPort);
    }

    private static MessageSent trySend(Message msg, InetSocketAddress destination)
    {
        MessageSent ev = new MessageSent();
        try
        {
            Sender.send(msg, destination);
        }
        catch (IOException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    private static MessageReceived tryReceive(InetSocketAddress source)
    {
        MessageReceived ev = new MessageReceived();
        try
        {
            ev.setMessage(Receiver.receive(source));
        }
        catch (IOException | ClassNotFoundException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    private static MessageReceived tryReceive(Socket sock)
    {
        MessageReceived ev = new MessageReceived();
        try
        {
            ev.setMessage(Receiver.receive(sock));
        }
        catch (IOException | ClassNotFoundException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    private ConnectionReceived tryAccept()
    {
        ConnectionReceived ev = new ConnectionReceived();
        try {
            ev.setSocket(listener.accept());
            listen();
        }
        catch (IOException e)
        {
            ev.setException(e);
        }
        return ev;
    }

    public void send(Message msg, InetSocketAddress destination)
    {
        this.executorCompletionService.submit(() -> Messenger.trySend(msg, destination));
    }

    public void receive(InetSocketAddress source)
    {
        this.executorCompletionService.submit(() -> Messenger.tryReceive(source));
    }

    public void receive(Socket sock)
    {
        this.executorCompletionService.submit(() -> Messenger.tryReceive(sock));
    }

    public void listen()
    {
        this.executorCompletionService.submit(() -> {
            return tryAccept();
        });
    }

    public Event getEvent()
    {
        try {
            Future<Event> ev = executorCompletionService.take();
            return ev.get();
        }
        catch(InterruptedException | ExecutionException ex)
        {
            return null;
        }
    }

    public Event getEventIfPresent()
    {
        try {
            Future<Event> e = executorCompletionService.poll();
            if (e != null)
                return e.get();
            else
                return null;
        }
        catch(InterruptedException | ExecutionException ex)
        {
            return null;
        }
    }

    public void exit()
    {
        if (!listener.isClosed())
        {
            try{
                listener.close();
            }
            catch(IOException e)
            {

            }
        }
        executorService.shutdown();
    }
}

package cs555.a2.nodes;

import cs555.a2.chord.messages.ChordMessageType;
import cs555.a2.chord.messages.LookupQuery;
import cs555.a2.transport.messenger.*;

import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class Peer
{
    public static void main(String[] args) throws ExecutionException, InterruptedException
    {
        Messenger m = new Messenger(44000, 4);
        m.listen();
        while(true)
        {
            Event e = m.getEvent();

            if (!e.causedException())
            {
                switch(e.getEventType())
                {
                    case MESSAGE_RECEIVED:
                        System.out.println("Received a message");
                        Message msg = ((MessageReceived) e).getMessage();
                        if (msg.getMessageType() == ChordMessageType.LOOKUP_QUERY)
                            System.out.println(((LookupQuery) msg).getId());
                        break;
                    case MESSAGE_SENT:
                        System.out.println("Sent a message");
                        break;
                    case CONNECTION_RECEIVED:
                        System.out.println("Received a connection");
                        Socket s = ((ConnectionReceived) e).getSocket();
                        m.receive(s);
                        break;
                }
            }
            else
            {
                System.out.println(e.getException().getMessage());
            }
        }
    }

}

package cs555.a2.transport;

import cs555.a2.transport.events.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Receiver
{
    public  static Message receive(InetSocketAddress source) throws IOException, ClassNotFoundException
    {
        try (Socket sock = new Socket(source.getAddress(), source.getPort());
             ObjectInputStream ins = new ObjectInputStream(sock.getInputStream()))
        {
            return (Message) ins.readObject();
        }
    }

    public static Message receive(Socket sock) throws IOException, ClassNotFoundException
    {
        try (Socket s = sock;
             ObjectInputStream ins = new ObjectInputStream(sock.getInputStream()))
        {
            return (Message) ins.readObject();
        }
    }
}

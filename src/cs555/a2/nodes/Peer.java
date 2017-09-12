package cs555.a2.nodes;

import cs555.a2.util.CRC16;

import java.util.Date;
import java.util.Scanner;

public class Peer
{
    public static void main(String args[])
    {
        CRC16 hasher = new CRC16();
        hasher.update(new Date().getTime());
        long hash = hasher.getValue();
        Scanner reader = new Scanner(System.in);
        while(true) {
            System.out.format("Generated hash %x. Override?%n", hash);
            String input = reader.nextLine();
            if (input.trim().isEmpty())
                break;
            else
            {
                try {
                    int hashOverride = Integer.parseInt(input, 16);
                    if (hashOverride >= 0xFFFF)
                        throw new IllegalArgumentException();
                    hash = hashOverride;
                    break;
                }
                catch(IllegalArgumentException e)
                {
                    System.out.println("Invalid hash entered. Please retry.");
                }
            }
        }
        System.out.format("Accepted hash %x.%n", hash);
    }
}

package cs555.a1.util;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public final class TokenParser
{
    private TokenParser() {}

    /**
     * Parses an argument as a decimal integer bounded by lower and upper
     * @param tok String representing an integer
     * @param lower Lower bound for integer (inclusive)
     * @param upper Upper bound for intger (inclusive)
     * @return An integer parsed from the argument string
     * @throws IllegalArgumentException If {@code tok} cannot be parsed into an integer, or if it exceeds bounds {@code lower} - {@code upper}
     * @throws NullPointerException If {@code tok} is null
     */
    public static int parseAsInt(String tok, int lower, int upper)
    {
        if (tok == null)
            throw new NullPointerException("null cannot be parsed as an integer");

        try {
            int i = Integer.parseInt(tok);
            if (i < lower || i > upper)
                throw new IllegalArgumentException(String.format("Parsed integer %1$d lies outside the specified range %2$d - %3$d", i, lower, upper));
            return i;
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(String.format("%1$s cannot be parsed as an integer", tok));
        }
    }

    /**
     * Parses an argument as a decimal integer
     * @param tok String representing an integer
     * @return The parsed integer
     * @throws IllegalArgumentException If {@code tok} cannot be parsed into an integer
     * @throws NullPointerException If {@code tok} is null
     */
    public static int parseAsInt(String tok)
    {
        return parseAsInt(tok, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Parses a string containing path into a File object after verifying that the file exists and is readable
     * @param tok String containing the path to the file
     * @return A File object encapsulating the path to a readable file
     * @throws IllegalArgumentException If a file does not exist at the path indicated by {@code tok}, or if it is not readable.
     * @throws NullPointerException If {@code tok} is null
     */
    public static File parseAsPath(String tok)
    {
        if (tok == null)
            throw new NullPointerException("null cannot be parsed as a path");
        File f = new File(tok);
        if (f.exists() && f.isFile())
        {
            if(f.canRead())
                return f;
            else
                throw new IllegalArgumentException(String.format("File %1$s is not readable", tok));
        }
        else
            throw new IllegalArgumentException(String.format("Path %1$s does not exist", tok));
    }

    /**
     * Parses the string as an IP address in the form of host:port
     * @param tok String representing the IP address
     * @return {@code InetSocketAddress} encapsulating the parsed IP address
     * @throws IllegalArgumentException If the string cannot be parsed as a valid IP address
     * @throws NullPointerException If {@code tok} is null
     */
    public static InetSocketAddress parseAsAddress(String tok)
    {
        if (tok == null)
            throw new NullPointerException("null cannot be parsed as an address");

        String[] parts = tok.split(":");
        if (parts.length == 2)
        {
            try {
                String host = parts[0].trim();
                int port = TokenParser.parseAsInt(parts[1].trim(), 0, 65535);
                return new InetSocketAddress(host, port);
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException(String.format("%1$s cannot be parsed as an address", tok));
            }
        }
        else
            throw new IllegalArgumentException(String.format("%1$s cannot be parsed as an address", tok));
    }
}

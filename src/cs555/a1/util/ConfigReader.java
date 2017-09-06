package cs555.a1.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public final class ConfigReader
{
    private ConfigReader() {}

    /**
     * Reads a file into a list of addresses
     * @param path The path to config file
     * @param ignoreErrors true if parsing should ignore errors. Errors include both parsing as well as address resolution failures.
     * @param checkResolved true if a parsed address should be checked to be resolved.
     * @return A list of addresses. null is returned if {@code ignoreErrors} is false and parsing fails, or if {@code checkResolved}
     * is true and the address cannot be resolved
     * @throws IllegalArgumentException if {@code path} is inaccessible
     * @throws NullPointerException if {@code path} is null
     */
    public static List<InetSocketAddress> read(String path, boolean ignoreErrors, boolean checkResolved)
    {
        File f = TokenParser.parseAsPath(path);
        List<InetSocketAddress> addresses = new ArrayList<>();

        try (
                FileInputStream fin = new FileInputStream(path);
                InputStreamReader ins = new InputStreamReader(fin);
                BufferedReader buf = new BufferedReader(ins)
        ) {
            String line;
            while ((line = buf.readLine()) != null)
            {
                try
                {
                    InetSocketAddress a = TokenParser.parseAsAddress(line);
                    if (checkResolved && a.isUnresolved() && !ignoreErrors)
                        return null;
                    else
                        addresses.add(a); // Add to the return list
                }
                catch(IllegalArgumentException e)
                {
                    if (!ignoreErrors)
                        return null;
                }
            }
        }
        catch(IOException e)
        {
            return null;
        }

        return addresses;
    }


}

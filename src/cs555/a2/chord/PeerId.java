package cs555.a2.chord;

import java.io.*;
import java.math.BigInteger;

public class PeerId implements Externalizable
{
    private BigInteger id;

    public PeerId()
    {
        this.id = null;
    }

    public PeerId(String hex)
    {
        try {
            this.id = new BigInteger(hex, 16);
        }
        catch(NumberFormatException ex)
        {
            throw new IllegalArgumentException(hex + " is not a valid hexadecimal hash value");
        }
    }

    public PeerId(BigInteger id)
    {
        this.id = id;
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        byte[] idAsBytes = this.id.toByteArray();
        out.write(idAsBytes.length);
        out.write(idAsBytes);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        byte[] idAsBytes = new byte[in.read()];
        in.read(idAsBytes);
        this.id = new BigInteger(idAsBytes);
    }

    public BigInteger getId()
    {
        if (id == null)
            throw new IllegalStateException("getId() called on an uninitialized PeerId");
        // BigInteger is immutable, so this is safe
        return id;
    }
}

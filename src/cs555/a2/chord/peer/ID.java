package cs555.a2.chord.peer;

import java.io.*;
import java.math.BigInteger;

public class ID implements Externalizable
{
    private BigInteger id;
    private int size;
    private BigInteger MOD_VAL;

    public ID()
    {
        this.id = null;
        this.size = 0;
        this.MOD_VAL = calculateModVal();
    }

    private BigInteger calculateModVal()
    {
        return BigInteger.valueOf(2).pow(this.size * Byte.SIZE);
    }

    public ID(String hex, int size)
    {
        try {
            this.size = size;
            this.MOD_VAL = calculateModVal();

            this.id = new BigInteger(hex, 16).mod(this.MOD_VAL);
        }
        catch(NumberFormatException ex)
        {
            throw new IllegalArgumentException(hex + " is not a valid hexadecimal ID value");
        }
    }

    public ID(BigInteger id, int size)
    {
        this.size = size;
        this.MOD_VAL = calculateModVal();
        this.id = id.mod(this.MOD_VAL);
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size);
        byte[] idAsBytes = this.id.toByteArray();
        out.writeInt(idAsBytes.length);
        out.write(idAsBytes);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.size = in.readInt();
        byte[] idAsBytes = new byte[in.readInt()];
        in.read(idAsBytes);
        this.MOD_VAL = calculateModVal();
        // No need to calculate .mod() since constructors guarantee a modular value
        this.id = new BigInteger(idAsBytes);
    }

    public BigInteger get()
    {
        if (this.id == null)
            throw new IllegalStateException("get() called on an uninitialized ID");
        // BigInteger is immutable, so this is safe
        return id;
    }

    public ID addModulo(BigInteger num)
    {
        if (this.id == null)
            throw new IllegalStateException("add() called on an uninitialized ID");

        return new ID(this.id.add(num).mod(MOD_VAL), this.size);
    }

    public int compareTo(ID id)
    {
        if (this.id == null)
            throw new IllegalStateException("compareTo() called on an uninitialized ID");
        if (this.size != id.size())
            throw new IllegalArgumentException("compareTo() called on different size IDs");

        return this.id.compareTo(id.get());
    }

    public boolean inInterval(ID end1, ID end2, boolean inclusive)
    {
        if (end1.size() != end2.size())
            throw new IllegalArgumentException("Endpoint IDs do not have matching sizes");
        else if (end1.size() != this.size())
            throw new IllegalArgumentException("Size of ID does not match that of the endpoints");

        if(end1.compareTo(end2) < 0)
            return inclusive ?
                   end1.compareTo(this) <= 0 && this.compareTo(end2) <= 0 :
                   end1.compareTo(this) < 0 && this.compareTo(end2) < 0;

        else if (end1.compareTo(end2) > 0)
            return inclusive ?
                   this.compareTo(end1) >= 0 || this.compareTo(end2) <= 0 :
                   this.compareTo(end1) > 0 || this.compareTo(end2) < 0;
        else
            return inclusive ? this.compareTo(end1) == 0 : false;
    }

    public boolean inInterval(ID end1, ID end2)
    {
        return inInterval(end1, end2, false);
    }

    public int size()
    {
        return this.size;
    }

    public int sizeInBits()
    {
        return this.size * Byte.SIZE;
    }

    @Override
    public String toString()
    {
        if (this.id == null)
            throw new IllegalStateException("toString() called on an uninitialized ID");
        return this.id.toString(16);
    }
}

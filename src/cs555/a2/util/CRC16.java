package cs555.a2.util;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CRC16 implements Checksum
{
    private CRC32 hasher = new CRC32();

    public CRC16()
    {
    }

    @Override
    public void update(int b)
    {
        hasher.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len)
    {
        hasher.update(b, off, len);
    }

    public void update(long b)
    {
        byte[] bytes = new byte[8];
        bytes[0] = (byte)((b >>> 56) & 0xFF);
        bytes[1] = (byte)((b >>> 48) & 0xFF);
        bytes[2] = (byte)((b >>> 40) & 0xFF);
        bytes[3] = (byte)((b >>> 32) & 0xFF);
        bytes[4] = (byte)((b >>> 24) & 0xFF);
        bytes[5] = (byte)((b >>> 16) & 0xFF);
        bytes[6] = (byte)((b >>>  8) & 0xFF);
        bytes[7] = (byte)((b >>>  0) & 0xFF);
        update(bytes, 0, bytes.length);
    }

    @Override
    public long getValue()
    {
        return hasher.getValue() & 0xffff;
    }

    @Override
    public void reset()
    {
        hasher.reset();
    }
}

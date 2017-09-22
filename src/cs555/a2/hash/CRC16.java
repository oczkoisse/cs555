package cs555.a2.hash;

import java.util.zip.CRC32;
import cs555.a2.util.ByteConverter;

public final class CRC16 implements Hash
{
    private CRC32 crc32 = new CRC32();

    public CRC16() {}

    @Override
    public void update(int b)
    {
        crc32.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len)
    {
        crc32.update(b, off, len);
    }

    @Override
    public byte[] getValue()
    {
        // Trimming CRC32 to 16 bits
        return ByteConverter.convert((short) (crc32.getValue() & 0xffff));
    }

    @Override
    public void reset()
    {
        crc32.reset();
    }

    @Override
    public int size() { return 2; }

}

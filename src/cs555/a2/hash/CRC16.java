package cs555.a2.hash;

import cs555.a2.util.ByteConverter;

public final class CRC16 implements Hash
{
    private sun.misc.CRC16 crc16 = new sun.misc.CRC16();

    @Override
    public void update(int b)
    {
        for(byte i: ByteConverter.convert(b))
            crc16.update(i);
    }

    @Override
    public void update(byte[] b, int off, int len)
    {
        for(byte i: b)
            crc16.update(i);
    }

    @Override
    public byte[] getValue()
    {
        return ByteConverter.convert((short) crc16.value);
    }

    @Override
    public void reset()
    {
        crc16.reset();
    }

    @Override
    public int size() { return 2; }

}

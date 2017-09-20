package cs555.a2.util;

import java.nio.ByteBuffer;

public class ByteConverter
{
    private final static ByteBuffer converter = ByteBuffer.allocate(Long.BYTES);

    private ByteConverter() {}

    public static byte[] convert(long val)
    {
        converter.clear();
        converter.putLong(val);
        byte[] out = new byte[Long.BYTES];
        converter.get(out);
        return out;
    }

    public static byte[] convert(int val)
    {
        converter.clear();
        converter.putInt(val);
        byte[] out = new byte[Integer.BYTES];
        converter.get(out);
        return out;
    }

    public static byte[] convert(short val)
    {
        converter.clear();
        converter.putShort(val);
        byte[] out = new byte[Short.BYTES];
        converter.get(out);
        return out;
    }
}

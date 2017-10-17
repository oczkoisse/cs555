package a3.io;

import org.apache.hadoop.io.Text;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class StringTuple extends Tuple<String>
{
    private static final String nullString = "\0\0";

    public StringTuple()
    {
        super();
    }

    public StringTuple(String... elements)
    {
        super(elements);
    }

    public StringTuple(List<String> elements)
    {
        super(elements);
    }

    @Override
    protected String readElement(DataInput in) throws IOException
    {
        String e = Text.readString(in);
        return e.equals(nullString) ? null : e;
    }

    @Override
    protected void writeElement(DataOutput out, String e) throws IOException
    {
        Text.writeString(out, e == null ? nullString : e);
    }
}

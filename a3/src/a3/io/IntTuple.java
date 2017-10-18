package a3.io;

import org.apache.hadoop.io.IntWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class IntTuple extends Tuple<Integer>
{
    public static final Integer NULL = Integer.MIN_VALUE;

    public IntTuple()
    {
        super();
    }

    /**
     * A fixed size list of Integers
     * @param elements one or more Integers to initialize this tuple
     */
    public IntTuple(Integer... elements)
    {
        super(elements);
    }

    public IntTuple(List<Integer> elements)
    {
        super(elements);
    }

    @Override
    protected Integer readElement(DataInput in) throws IOException
    {
        IntWritable iw = new IntWritable();
        iw.readFields(in);
        Integer i = iw.get();
        return i.equals(NULL) ? null : i;
    }

    @Override
    protected void writeElement(DataOutput out, Integer i) throws IOException
    {
        IntWritable iw = new IntWritable(i == null ? NULL : i);
        iw.write(out);
    }
}
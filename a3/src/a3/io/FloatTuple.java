package a3.io;

import org.apache.hadoop.io.FloatWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class FloatTuple extends Tuple<Float> {
	
	public FloatTuple()
	{
		super();
	}
	
	/**
	 * A fixed size list of Floats
	 * @param elements one or more Floats to initialize this tuple
	 */
	public FloatTuple(Float... elements)
	{
		super(elements);
	}

	public FloatTuple(List<Float> elements)
    {
        super(elements);
    }

	@Override
	protected Float readElement(DataInput in) throws IOException
	{
	    FloatWritable fw = new FloatWritable();
	    fw.readFields(in);
	    Float f = fw.get();
		return Float.isNaN(f) ? null : f;
	}

	@Override
	protected void writeElement(DataOutput out, Float f) throws IOException
	{
        FloatWritable fw = new FloatWritable(f == null ? Float.NaN : f);
        fw.write(out);
	}
}
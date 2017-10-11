package a3.jobs.artist;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class ArtistCombiner extends Reducer<Text, IntWritable, Text, IntWritable>
{
    @Override
    protected void reduce(Text artist, Iterable<IntWritable> counts, Context context) throws IOException, InterruptedException
    {
        int total = 0;
        for(IntWritable i: counts)
            total += i.get();
        context.write(artist, new IntWritable(total));
    }
}

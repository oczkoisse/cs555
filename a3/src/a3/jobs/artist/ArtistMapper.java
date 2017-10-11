package a3.jobs.artist;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class ArtistMapper extends Mapper<Object, Text, Text, IntWritable>
{
    @Override
    protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
    {
        String artist = contents.toString().split("\\t")[11];
        context.write(new Text(artist), new IntWritable(1));
    }
}

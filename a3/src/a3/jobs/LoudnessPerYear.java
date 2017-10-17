package a3.jobs;

import a3.data.Data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class LoudnessPerYear
{
    public static class LoudnessPerYearMapper extends Mapper<Object, Text, IntWritable, FloatWritable>
    {
        private static final FloatWritable loudness = new FloatWritable(0.0f);
        private static final IntWritable year = new IntWritable(0);

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            Float l = d.getLoudness();
            Integer y = d.getYear();
            if (d.isValid() && y != null && !y.equals(0) && l != null) {
                year.set(y);
                loudness.set(l);
                context.write(year, loudness);
            }
        }
    }

    public static class LoudnessPerYearReducer extends Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable>
    {
        private static final FloatWritable averageLoudness = new FloatWritable(0.0f);

        @Override
        protected void reduce(IntWritable year, Iterable<FloatWritable> loudnesses, Context context) throws IOException, InterruptedException
        {
            averageLoudness.set(0.0f);

            float loudnessSum = 0.0f;
            int count = 0;

            for(FloatWritable l: loudnesses)
            {
                loudnessSum += l.get();
                count++;
            }

            if (count != 0)
                averageLoudness.set(loudnessSum / count);

            context.write(year, averageLoudness);
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q6");
            // Current class.
            job.setJarByClass(LoudnessPerYear.class);
            // Mapper
            job.setMapperClass(LoudnessPerYear.LoudnessPerYearMapper.class);
            // Reducer
            job.setReducerClass(LoudnessPerYear.LoudnessPerYearReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(IntWritable.class);
            job.setMapOutputValueClass(FloatWritable.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(IntWritable.class);
            job.setOutputValueClass(FloatWritable.class);

            // path to input in HDFS
            FileInputFormat.addInputPath(job, new Path(args[0]));
            // path to output in HDFS
            FileOutputFormat.setOutputPath(job, new Path(args[1]));
            // Block until the job is completed.
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }
}

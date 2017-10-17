package a3.jobs;

import a3.data.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TempoDanceability
{
    public static class TempoDanceabilityMapper extends Mapper<Object, Text, FloatWritable, FloatWritable>
    {
        private final FloatWritable tempo = new FloatWritable(0.0f);
        private final FloatWritable danceability = new FloatWritable(0.0f);

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (d.isValid()) {
                danceability.set(d.getDanceability());
                tempo.set(d.getTempo());
                context.write(danceability, tempo);
            }
        }
    }

    public static class TempoDanceabilityReducer extends Reducer<FloatWritable, FloatWritable, Text, DoubleWritable>
    {
        private List<Float> danceabilities = new ArrayList<>();
        private int tempoCount = 0;
        private double tempoSum = 0.0f;

        @Override
        protected void setup(Context context)
        {
            danceabilities = new ArrayList<>();
            tempoCount = 0;
            tempoSum = 0;
        }

        private static <T extends Number> Double getMedian(List<T> list)
        {
            if (list.size() == 0)
                throw new IllegalArgumentException("Cannot find median of a list with size 0");

            int mid = list.size() / 2;
            T m = list.get(mid);
            if (list.size() % 2 == 0)
            {
                T m1 = list.get(mid - 1);
                return m.doubleValue() + m1.doubleValue() / 2.0;
            }
            return m.doubleValue();
        }

        @Override
        protected void reduce(FloatWritable danceability, Iterable<FloatWritable> tempos, Context context) throws IOException, InterruptedException
        {
            // Should already be sorted
            danceabilities.add(danceability.get());

            for(FloatWritable tempo: tempos)
            {
                tempoSum += tempo.get();
                tempoCount++;
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException
        {
            context.write(new Text("Average Tempo"), new DoubleWritable(tempoSum / tempoCount));
            context.write(new Text("Median Danceability: "), new DoubleWritable(getMedian(danceabilities)));
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q2_3");
            // Current class.
            job.setJarByClass(TempoDanceability.class);
            // Mapper
            job.setMapperClass(TempoDanceabilityMapper.class);
            // Reducer
            job.setReducerClass(TempoDanceabilityReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(FloatWritable.class);
            job.setMapOutputValueClass(FloatWritable.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(DoubleWritable.class);

            job.setNumReduceTasks(1);
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
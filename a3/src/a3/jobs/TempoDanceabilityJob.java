package a3.jobs;

import a3.data.Data;
import a3.io.FloatTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class TempoDanceabilityJob
{
    public static class TempoDanceabilityMapper extends Mapper<Object, Text, ByteWritable, FloatTuple>
    {
        private static final ByteWritable one = new ByteWritable((byte)1);

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (!d.isHeader()) {
                Float tempo = d.getTempo();
                Float danceability = d.getDanceability();
                if (tempo != null && danceability != null)
                    context.write(one, new FloatTuple(d.getTempo(), d.getDanceability()));
            }
        }
    }

    public static class TempoDanceabilityReducer extends Reducer<ByteWritable, FloatTuple, Text, FloatWritable>
    {
        @Override
        protected void reduce(ByteWritable one, Iterable<FloatTuple> tempoDanceability, Context context) throws IOException, InterruptedException
        {
            float tempo = 0.0f;

            float median = 0.0f;

            int size = 0;
            for(FloatTuple td: tempoDanceability)
            {
                tempo += td.get(0);
                size++;
            }

            tempo /= size;

            int i = 0;
            for(FloatTuple td: tempoDanceability)
            {
                if (size % 2 == 0)
                {
                    if (i == size/2 - 1)
                        median += td.get(1);
                    else if (i == size/2)
                    {
                        median += td.get(1);
                        median /= 2.0f;
                        median /= 2.0f;
                        break;
                    }
                }
                else if (i == size/2)
                {
                    median = td.get(1);
                    break;
                }
                i++;
            }

            context.write(new Text("Average Tempo: "), new FloatWritable(tempo));
            context.write(new Text("Median Danceability: "), new FloatWritable(median));
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q2_3");
            // Current class.
            job.setJarByClass(TempoDanceabilityJob.class);
            // Mapper
            job.setMapperClass(TempoDanceabilityMapper.class);
            // Reducer
            job.setReducerClass(TempoDanceabilityReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(ByteWritable.class);
            job.setMapOutputValueClass(FloatTuple.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(FloatWritable.class);

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

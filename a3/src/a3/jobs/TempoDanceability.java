package a3.jobs;

import a3.data.Data;
import a3.io.FloatTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TempoDanceability
{
    public static class TempoDanceabilityMapper extends Mapper<Object, Text, NullWritable, FloatTuple>
    {
        private static final FloatTuple tempoAndDanceability = new FloatTuple(null, null);

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            NullWritable nw = NullWritable.get();
            if (d.isValid()) {
                tempoAndDanceability.set(0, d.getTempo());
                tempoAndDanceability.set(1, d.getDanceability());
                context.write(nw, tempoAndDanceability);
            }
        }
    }

    public static class TempoDanceabilityReducer extends Reducer<NullWritable, FloatTuple, Text, FloatWritable>
    {
        @Override
        protected void reduce(NullWritable x, Iterable<FloatTuple> tempoDanceability, Context context) throws IOException, InterruptedException
        {
            float tempoAverage = 0.0f;
            int tempoCount = 0;
            float danceabilityMedian = 0.0f;
            List<Float> danceabilities = new ArrayList<>();

            for(FloatTuple td: tempoDanceability)
            {
                Float tempo = td.get(0);
                if (tempo != null)
                {
                    tempoAverage += tempo;
                    tempoCount++;
                }

                Float danceability = td.get(1);
                if (danceability != null)
                    danceabilities.add(danceability);
            }

            if (tempoCount != 0)
                tempoAverage /= tempoCount;

            if(danceabilities.size() != 0)
            {
                Collections.sort(danceabilities);
                int mid = danceabilities.size() / 2;
                if (danceabilities.size() % 2 == 0)
                    danceabilityMedian = (danceabilities.get(mid) + danceabilities.get(mid - 1)) / 2.0f;
                else
                    danceabilityMedian = danceabilities.get(mid);
            }

            context.write(new Text("Average Tempo: "), new FloatWritable(tempoAverage));
            context.write(new Text("Median Danceability: "), new FloatWritable(danceabilityMedian));
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
            job.setMapOutputKeyClass(NullWritable.class);
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

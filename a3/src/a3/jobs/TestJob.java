package a3.jobs;

import a3.data.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.List;

public class TestJob {

    public static class TestJobMapper extends Mapper<Object, Text, NullWritable, Text>
    {
        @Override
        public void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (d.isValid())
            {
                List<String> popularTerms = d.getPopularArtistTerms();
                if (popularTerms == null)
                    context.write(NullWritable.get(), contents);
            }
        }
    }

    public static class TestJobReducer extends Reducer<NullWritable, Text, NullWritable, Text>
    {
        @Override
        public void reduce(NullWritable nw, Iterable<Text> contents, Context context) throws IOException, InterruptedException
        {
            for(Text content: contents)
                context.write(nw, content);
        }
    }

    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q4");
            // Current class.
            job.setJarByClass(TestJob.class);
            // Mapper
            job.setMapperClass(TestJob.TestJobMapper.class);
            // Reducer
            job.setReducerClass(TestJob.TestJobReducer.class);

            // Outputs from the Mapper.
            job.setMapOutputKeyClass(NullWritable.class);
            job.setMapOutputValueClass(Text.class);
            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(NullWritable.class);
            job.setOutputValueClass(Text.class);

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

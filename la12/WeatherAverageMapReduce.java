import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.StringTokenizer;

public class WeatherAverage {

    // Custom Writable class can be defined here if required, but we use Text for key and a composite value as String for simplicity

    // Mapper Class: Processes each line and emits a composite key and a value for each variable.
    public static class WeatherMapper extends Mapper<Object, Text, Text, Text> {

        private Text variableKey = new Text();
        // The value string will contain the numerical value and a count "1"
        private Text valueAndCount = new Text();

        // Example of input record (assumed CSV format):
        // Date,Temperature,DewPoint,WindSpeed,...
        // 2025-04-01,20.5,10.0,5.5,...
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            // Skip header line if present
            String line = value.toString().trim();
            if (line.startsWith("Date") || line.isEmpty()) {
                return;
            }
            // Tokenize assuming comma separated fields.
            String[] tokens = line.split(",");
            // Expecting tokens[1] = Temperature, tokens[2] = DewPoint, tokens[3] = WindSpeed.
            try {
                double temp = Double.parseDouble(tokens[1]);
                double dew = Double.parseDouble(tokens[2]);
                double wind = Double.parseDouble(tokens[3]);
                // Emit for Temperature, DewPoint, and WindSpeed
                context.write(new Text("Temperature"), new Text(temp + ",1"));
                context.write(new Text("DewPoint"), new Text(dew + ",1"));
                context.write(new Text("WindSpeed"), new Text(wind + ",1"));
            } catch (NumberFormatException e) {
                // Skip invalid record
            }
        }
    }

    // Reducer Class: Sums up the values and counts for each variable and calculates the average.
    public static class AverageReducer extends Reducer<Text, Text, Text, DoubleWritable> {
        private DoubleWritable averageWritable = new DoubleWritable();

        public void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            double sum = 0.0;
            long count = 0;
            for (Text val : values) {
                // Each value is in the form "value,count"
                String[] parts = val.toString().split(",");
                sum += Double.parseDouble(parts[0]);
                count += Long.parseLong(parts[1]);
            }
            double avg = (count == 0) ? 0.0 : sum / count;
            averageWritable.set(avg);
            context.write(key, averageWritable);
        }
    }

    // Main Driver: Configures and runs the job
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: WeatherAverage <input path> <output path>");
            System.exit(-1);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Weather Average Calculation");
        job.setJarByClass(WeatherAverage.class);
        job.setMapperClass(WeatherMapper.class);
        job.setReducerClass(AverageReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

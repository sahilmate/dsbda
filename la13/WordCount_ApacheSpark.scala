import org.apache.spark.{SparkConf, SparkContext}

object WordCount {
  def main(args: Array[String]): Unit = {
    // Create a configuration and SparkContext
    val conf = new SparkConf()
      .setAppName("WriteWordCount")
      .setMaster("local[*]") // Use local mode with all available cores
    val sc = new SparkContext(conf)
    
    // Read the input file from local system or HDFS
    // Provide the path to the input dataset file
    val inputFile = "input.txt"
    val textFile = sc.textFile(inputFile)
    
    // Split each line into words, map each word to (word, 1) pair and reduce by key to count occurrences
    val counts = textFile
      .flatMap(line => line.split("\\s+"))
      .map(word => (word, 1))
      .reduceByKey(_ + _)
    
    // Save the result to the output directory
    val outputDir = "output"
    counts.saveAsTextFile(outputDir)
    
    // Stop the SparkContext
    sc.stop()
  }
}

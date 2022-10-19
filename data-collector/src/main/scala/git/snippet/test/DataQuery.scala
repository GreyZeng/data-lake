package git.snippet.test

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object DataQuery {

  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sparkConf = new SparkConf().setAppName("MyFirstDataApp")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .setMaster("local[*]")
    val sparkSession = SparkSession.builder().config(sparkConf).enableHiveSupport().getOrCreate()
    val ssc = sparkSession.sparkContext
    ssc.hadoopConfiguration.set("dfs.client.use.datanode.hostname", "true")
    queryData(sparkSession)
  }


  def queryData(sparkSession: SparkSession) = {
    val df = sparkSession.read.format("org.apache.hudi")
      .load("/snippet/data/hudi/*/*")
    df.show()
    println(df.count())
  }
}

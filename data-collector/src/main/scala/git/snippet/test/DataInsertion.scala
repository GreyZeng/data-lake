package git.snippet.test


import git.snippet.entity.MyEntity
import git.snippet.util.JsonUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}

object DataInsertion {

  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sparkConf = new SparkConf().setAppName("MyFirstDataApp")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .setMaster("local[*]")
    val sparkSession = SparkSession.builder().config(sparkConf).enableHiveSupport().getOrCreate()
    val ssc = sparkSession.sparkContext
    ssc.hadoopConfiguration.set("dfs.client.use.datanode.hostname", "true")
    insertData(sparkSession)
  }

  /**
   * {'uid':1,'uname':'xiaobai','dt':'2020/08'}
   * {'uid':2,'uname':'xiaohong','dt':'2020/09'}
   *
   * @param sparkSession
   * @return
   */
  def insertData(sparkSession: SparkSession) = {
    import org.apache.spark.sql.functions._
    import sparkSession.implicits._
    val commitTime = System.currentTimeMillis().toString //生成提交时间
    val df = sparkSession.read.text("/mydata/data1")
      .mapPartitions(partitions => {
        partitions.map(item => {
          val jsonObject = JsonUtil.getJsonData(item.getString(0))
          MyEntity(jsonObject.getIntValue("uid"), jsonObject.getString("uname"), jsonObject.getString("dt"))
        })
      })
    val result = df.withColumn("ts", lit(commitTime)) //添加ts 时间戳列
      .withColumn("uuid", col("uid"))
      .withColumn("hudipart", col("dt")) //增加hudi分区列
    result.write.format("org.apache.hudi")
      .option("hoodie.insert.shuffle.parallelism", 2)
      .option("hoodie.upsert.shuffle.parallelism", 2)
      .option("PRECOMBINE_FIELD_OPT_KEY", "ts") //指定提交时间列
      .option("RECORDKEY_FIELD_OPT_KEY", "uuid") //指定uuid唯一标示列
      .option("hoodie.table.name", "myDataTable")
      .option("hoodie.datasource.write.partitionpath.field", "hudipart") //分区列
      .mode(SaveMode.Overwrite)
      .save("/snippet/data/hudi")
  }
}

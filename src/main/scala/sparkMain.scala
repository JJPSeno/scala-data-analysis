package com.example

import org.apache.spark.sql.{SparkSession, functions => F}
import org.apache.spark.sql.types._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.SaveMode

// object SparkMain extends App {
object SparkMain {
  val filePath = "data/USDJPY30.csv"

  val spark = SparkSession.builder()
    .appName("Spark Test")
    .master("local[*]") // Run locally with all cores
    .getOrCreate()

  // Define schema for the dataset
  val schema = StructType(Seq(
    StructField("time", StringType, nullable = false),
    StructField("open", DoubleType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("close", DoubleType, nullable = false),
    StructField("volume", IntegerType, nullable = false)
  ))

  // Load the data
  val data = spark.read
    .option("delimiter", "\t")
    .option("header", "true")
    .option("inferSchema", "true")
    .csv(filePath)

  // Example Analysis: Compute moving average of "close" price
  val movingAvg = data
    .withColumn("moving_avg", F.avg("close").over(
      Window.orderBy("time").rowsBetween(-2, 0)
    ))

  // Show results
  movingAvg.show()

  // Save processed data (optional)
  movingAvg.write.mode(SaveMode.Overwrite).csv("output/moving_avg")

  // Stop SparkSession
  spark.stop()

}

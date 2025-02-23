import utils._
import org.apache.spark.sql.{ SparkSession, DataFrame }
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types.{ StructField, StructType, StringType }

object SparkForexAnomalyDetector extends App {
  // Initialize Spark session
  val spark = SparkSession.builder()
    .appName("Spark Anomaly Detector")
    .master("local[*]")  // Use all available cores in local mode
    .getOrCreate()
  
  import spark.implicits._
  
  val startTime = System.nanoTime()
  
  // Schema definition for the input data
  case class Transaction(
    timestamp: String,
    transactionId: String,
    accountId: String,
    amount: Double,
    merchant: String,
    transactionType: String,
    location: String
  )
  
  case class Anomaly(
    transaction: Transaction,
    anomalyTypes: List[String],
    reason: String
  )

def detectAnomaliesWithZScore(filePath: String): DataFrame = {
      import spark.implicits._
      
      // Define schema
      val schema = StructType(Array(
          StructField("timestamp", StringType, true),
          StructField("transactionId", StringType, true),
          StructField("accountId", StringType, true),
          StructField("amount", StringType, true),
          StructField("merchant", StringType, true),
          StructField("transactionType", StringType, true),
          StructField("location", StringType, true)
      ))

      // Read and process the data
      val df = spark.read
          .option("header", "true")
          .option("inferSchema", "false")
          .option("timestampFormat", "MM-dd-yyyy HH:mm")
          .schema(schema)
          .csv(filePath)
          .withColumn("amount", regexp_replace(col("amount"), ",", "").cast("double"))
          .filter(col("amount").isNotNull)
          .coalesce(8)
          .cache()
      val startProcessingTime = System.nanoTime()
      // Calculate statistics using window functions
      val windowSpec = Window.partitionBy("accountId")
      
      val enrichedDF = df
        .withColumn("mean", avg("amount").over(windowSpec))
        .withColumn("stddev", stddev("amount").over(windowSpec))
        .withColumn("zscore", 
          when(col("stddev") > 0, abs((col("amount") - col("mean")) / col("stddev")))
          .otherwise(lit(0)))
      
      // Detect anomalies
      val anomaliesDF = enrichedDF
        .withColumn("is_zscore_anomaly", col("zscore") > 3)
        .withColumn("is_amount_anomaly", col("amount") > 50000)
        .withColumn("anomaly_types", 
          array_remove(
            array(
              when(col("is_zscore_anomaly"), "Z-Score Anomaly").otherwise(null),
              when(col("is_amount_anomaly"), "High Amount").otherwise(null)
            ), 
            null
          ))
        .withColumn("reason",
          concat_ws("; ",
            when(col("is_zscore_anomaly"),
              concat(lit("Transaction amount "), col("amount"),
                    lit(" (z-score: "), format_number(col("zscore"), 2),
                    lit(") is unusual."))),
            when(col("is_amount_anomaly"),
              concat(lit("Transaction amount "), col("amount"),
              lit(" exceeds threshold.")))
          ))
        .filter(
          col("is_zscore_anomaly") || col("is_amount_anomaly")
        )
        .select(
          col("timestamp"),
          col("transactionId"),
          col("accountId"),
          col("amount"),
          col("merchant"),
          col("transactionType"),
          col("location"),
          col("anomaly_types"),
          col("reason")
        )

      val ret = anomaliesDF
      val endProcessingTime = System.nanoTime()
      val processingTimeDifference = (endProcessingTime - startProcessingTime) / 1e9 // Convert to seconds
      println(s"Processing time taken: $processingTimeDifference s")
      ret
  }

  try {
    val anomaliesDF = detectAnomaliesWithZScore(filePath)
    
    println("=== Detected Anomalies ===")
    
    // Display detailed anomalies
    // anomaliesDF.collect().foreach { row =>
    //   println(s"""
    //     |Time: ${row.getAs[String]("timestamp")}
    //     |Types: ${row.getAs[Seq[String]]("anomaly_types").mkString(", ")}
    //     |Reason: ${row.getAs[String]("reason")}
    //     |Transaction Details:
    //     |  TransactionID: ${row.getAs[String]("transactionId")}
    //     |  AccountID: ${row.getAs[String]("accountId")}
    //     |  Amount: ${row.getAs[Double]("amount")}
    //     |  Merchant: ${row.getAs[String]("merchant")}
    //     |  TransactionType: ${row.getAs[String]("transactionType")}
    //     |  Location: ${row.getAs[String]("location")}
    //     |${"-" * 50}
    //     """.stripMargin)
    //}

    // Calculate and display summary statistics
    println("\n=== Summary ===")
    val totalAnomalies = anomaliesDF.count()
    println(s"Total anomalies detected: $totalAnomalies")
    
    println("\nAnomaly type breakdown:")
    anomaliesDF
      .select(explode(col("anomaly_types")).as("type"))
      .groupBy("type")
      .count()
      .collect()
      .foreach { row =>
        println(f"${row.getString(0)}%-15s: ${row.getLong(1)}")
      }

    val endTime = System.nanoTime()
    val timeDifference = (endTime - startTime) / 1e9
    println(s"Time taken: $timeDifference s")
    
  } catch {
    case e: Exception =>
      println(s"Error processing file: ${e.getMessage}")
      e.printStackTrace()
  } finally {
    spark.stop()
  }
}
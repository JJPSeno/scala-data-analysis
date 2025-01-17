package example

import scalaj.http._
import java.io.{ File, FileReader }
import java.io.{ FileWriter, PrintWriter }
import java.nio.file.{ Files, Paths }
import java.time.LocalDateTime
import com.opencsv.CSVReader
import scala.collection.JavaConverters._
import java.time.format.DateTimeFormatter
import scala.collection.mutable.ArrayBuffer

// First, let's define our data structure
case class OHLCData(
  timestamp: LocalDateTime,
  open: Double,
  high: Double,
  low: Double,
  close: Double,
  volume: Int
)


case class AnomalyResult(
  data: OHLCData,
  anomalyTypes: List[String],
  reason: String
)

class ForexAnomalyDetector(windowSize: Int = 5) {
  private val priceChangeThreshold = 0.003 // 0.3% change threshold
  private val volumeMultipleThreshold = 2.0 // 2x average volume threshold
  
  def detectAnomalies(filePath: String): List[AnomalyResult] = {
    val reader = new CSVReader(new FileReader(filePath))
    try {
      // Read and parse data
      val rawData = reader.readAll().asScala.toList  // Skip header
      println("rawData", rawData)
      val data = parseData(rawData)
      
      // Process data in windows and detect anomalies
      data.sliding(windowSize).flatMap { window =>
        val current = window.last
        detectAnomaliesInWindow(current, window.init)
      }.toList
      
    } finally {
      reader.close()
    }
  }
  
  private def parseData(rows: List[Array[String]]): List[OHLCData] = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    println("rows",rows)
    
    rows.map { row =>
      // println("row",row)
      // val dateTimeParts = row(0).split("\\s+").take(2)
      // val datetime = dateTimeParts.mkString(" ")
    
      // // println("row", row)
      // println("datetime", datetime)
      // // println("row(0).trim", row(0).trim)
      // println("row",row)
      // Split the single string into fields using tab as delimiter
      val fields = row(0).split("\t")
      val datetime = fields(0).trim

      OHLCData(
        LocalDateTime.parse(datetime, formatter),
        fields(1).trim.toDouble,
        fields(2).trim.toDouble,
        fields(3).trim.toDouble,
        fields(4).trim.toDouble,
        fields(5).trim.toInt
      )
    }
  }
  
  private def detectAnomaliesInWindow(
    current: OHLCData, 
    previousCandles: List[OHLCData]
  ): Option[AnomalyResult] = {
    val anomalies = ArrayBuffer[String]()
    val reasons = ArrayBuffer[String]()
    
    // Calculate baseline metrics
    val avgVolume = previousCandles.map(_.volume).sum / previousCandles.size
    val avgRange = previousCandles.map(c => c.high - c.low).sum / previousCandles.size
    val priceRange = current.high - current.low
    
    // 1. Check for price spikes
    val priceChange = math.abs(current.close - current.open) / current.open
    if (priceChange > priceChangeThreshold) {
      anomalies += "price_spike"
      reasons += f"Price change of ${priceChange * 100}%.2f%% exceeds threshold of ${priceChangeThreshold * 100}%.2f%%"
    }
    
    // 2. Check for volume anomalies
    if (current.volume > avgVolume * volumeMultipleThreshold) {
      anomalies += "volume_spike"
      reasons += f"Volume of ${current.volume} is ${current.volume / avgVolume}%.2fx average volume"
    }
    
    // 3. Check for gaps between candles
    previousCandles.lastOption.foreach { prev =>
      val gap = math.abs(current.open - prev.close)
      if (gap > avgRange) {
        anomalies += "price_gap"
        reasons += f"Gap of $gap pips between candles"
      }
    }
    
    // 4. Check for unusual volatility
    val volatility = priceRange / current.open
    val avgVolatility = previousCandles.map(c => (c.high - c.low) / c.open).sum / previousCandles.size
    if (volatility > avgVolatility * 2) {
      anomalies += "high_volatility"
      reasons += f"Volatility ${volatility * 100}%.2f%% is more than 2x average"
    }
    
    if (anomalies.nonEmpty) {
      Some(AnomalyResult(
        current,
        anomalies.toList,
        reasons.mkString("; ")
      ))
    } else None
  }
}

object Main extends App {
  // val response = Http("https://jsonplaceholder.typicode.com/posts/1").asString
  // println("Response Code: " + response.code)
  // println("Response Body: " + response.body)
    // Define the path to the CSV file
  val filePath = "data/USDJPY30.csv"

  // val reader = new CSVReader(new FileReader(filePath))
  // try {
  //   val allRows = reader.readAll()
  //   allRows.forEach(row => {
  //     println(row.mkString(", "))
  //   })
  // } catch {
  //   case e: Exception =>
  //     println(s"Error reading CSV file: ${e.getMessage}")
  // } finally {
  //   reader.close()
  // }
  
  val startTime = System.nanoTime()
  val detector = new ForexAnomalyDetector()
  
  try {
    val anomalies = detector.detectAnomalies(filePath)
    
    println("=== Detected Anomalies ===")
    anomalies.foreach { anomaly =>
      println(s"""
        |Time: ${anomaly.data.timestamp}
        |Types: ${anomaly.anomalyTypes.mkString(", ")}
        |Reason: ${anomaly.reason}
        |OHLC: ${anomaly.data.open}, ${anomaly.data.high}, ${anomaly.data.low}, ${anomaly.data.close}
        |Volume: ${anomaly.data.volume}
        |${"-" * 50}
        """.stripMargin)
    }
    
    // Print summary statistics
    println("\n=== Summary ===")
    println(s"Total anomalies detected: ${anomalies.size}")
    
    // Group anomalies by type
    val anomalyTypes = anomalies.flatMap(_.anomalyTypes)
      .groupBy(identity)
      .map { case (type_, occurrences) => (type_, occurrences.size) }
    
    println("\nAnomaly type breakdown:")
    anomalyTypes.foreach { case (type_, count) =>
      println(f"$type_%-15s: $count")
    }
    val endTime = System.nanoTime()
    val timeDifference = (endTime - startTime) / 1e6 // Convert to milliseconds
    println(s"Time taken: $timeDifference ms")
  } catch {
    case e: Exception =>
      println(s"Error processing file: ${e.getMessage}")
      e.printStackTrace()
  }
  
}
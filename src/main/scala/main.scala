import com.opencsv.CSVReader
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable
import scala.collection.JavaConverters._

// Case class representing transaction data
case class Transaction(
  timestamp: Date,
  transactionId: String,
  accountId: String,
  amount: Double,
  merchant: String,
  transactionType: String,
  location: String
)

case class Anomaly(
  data: Transaction,
  anomalyTypes: List[String],
  reason: String
)

class ForexAnomalyDetector {

  private val dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm")

  // Reads CSV data and parses it into a list of Transaction objects
  private def readData(filePath: String): List[Transaction] = {
    val reader = new CSVReader(new FileReader(filePath))
    val rows = reader.readAll().asScala.toList.tail
    reader.close()

    rows.map { row =>
      val datetime = row(0).trim
      Transaction(
        timestamp = dateFormat.parse(datetime),
        transactionId = row(1),
        accountId = row(2),
        amount = row(3).toDouble,
        merchant = row(4),
        transactionType = row(5),
        location = row(6)
      )
    }.toList
  }

  // Detects anomalies in the given transactions
def detectAnomaliesWithZScore(filePath: String): List[Anomaly] = {
  val data = readData(filePath)
  val anomalies = mutable.ListBuffer[Anomaly]()

  data.groupBy(_.accountId).foreach { case (_, transactions) =>
    val amounts = transactions.map(_.amount)
    val mean = amounts.sum / amounts.size
    val stdDev = math.sqrt(amounts.map(a => math.pow(a - mean, 2)).sum / amounts.size)

    transactions.foreach { transaction =>
      val zScore = if (stdDev > 0) (transaction.amount - mean) / stdDev else 0.0
      val anomalyTypes = mutable.ListBuffer[String]()
      val reasons = mutable.ListBuffer[String]()

      // Check for z-score anomalies
      if (math.abs(zScore) > 3) {
        anomalyTypes += "Z-Score Anomaly"
        reasons += f"Transaction amount ${transaction.amount} (z-score: $zScore%.2f) is unusual."
      }

      // Retain existing checks (e.g., high amount, location changes)
      if (transaction.amount > 50000) {
        anomalyTypes += "High Amount"
        reasons += s"Transaction amount ${transaction.amount} exceeds threshold."
      }

      if (anomalyTypes.nonEmpty) {
        anomalies += Anomaly(transaction, anomalyTypes.toList, reasons.mkString("; "))
      }
    }
  }

  anomalies.toList
}
}

object Main extends App {
  val startTime = System.nanoTime()
  val detector = new ForexAnomalyDetector()
  val filePath = "data/financial_anomaly_data2.csv"
  try {
    val anomalies = detector.detectAnomaliesWithZScore(filePath)
    val endTime = System.nanoTime()
    val timeDifference = (endTime - startTime) / 1e6 // Convert to milliseconds

    println("=== Detected Anomalies ===")
    anomalies.foreach { anomaly =>
      println(s"""
        |Time: ${anomaly.data.timestamp}
        |Types: ${anomaly.anomalyTypes.mkString(", ")}
        |Reason: ${anomaly.reason}
        |Transaction Details:
        |  TransactionID: ${anomaly.data.transactionId}
        |  AccountID: ${anomaly.data.accountId}
        |  Amount: ${anomaly.data.amount}
        |  Merchant: ${anomaly.data.merchant}
        |  TransactionType: ${anomaly.data.transactionType}
        |  Location: ${anomaly.data.location}
        |${"-" * 50}
        """.stripMargin)
    }
    println("\n=== Summary ===")
    println(s"Total anomalies detected: ${anomalies.size}")
    val anomalyTypes = anomalies.flatMap(_.anomalyTypes)
      .groupBy(identity)
      .map { case (type_, occurrences) => (type_, occurrences.size) }
    println("\nAnomaly type breakdown:")
    anomalyTypes.foreach { case (type_, count) =>
      println(f"$type_%-15s: $count")
    }
    println(s"Time taken: $timeDifference ms")
  } catch {
    case e: Exception =>
      println(s"Error processing file: ${e.getMessage}")
      e.printStackTrace()
  }
}
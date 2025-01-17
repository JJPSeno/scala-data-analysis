// package example

// import scalaj.http._
// import java.io.{ File, FileReader }
// import java.io.{ FileWriter, PrintWriter }
// import java.nio.file.{ Files, Paths }
// import java.time.LocalDateTime
// import com.opencsv.CSVReader
// import scala.collection.JavaConverters._
// import java.time.format.DateTimeFormatter
// import scala.collection.mutable.ArrayBuffer
// import java.awt.{Color, Dimension, Graphics, Graphics2D, BasicStroke, RenderingHints}
// import javax.swing.{JFrame, JPanel}
// import javax.swing.WindowConstants._
// import scala.math.{min, max}

// // First, let's define our data structure
// case class OHLCData(
//   timestamp: LocalDateTime,
//   open: Double,
//   high: Double,
//   low: Double,
//   close: Double,
//   volume: Int
// )

// case class AnomalyResult(
//   data: OHLCData,
//   anomalyTypes: List[String],
//   reason: String
// )

// case class OHLCChart(data: List[OHLCData], anomalies: List[AnomalyResult]) extends JPanel {
//   private val padding = 50
//   private val candleWidth = 10
//   private val candleSpacing = 5
  
//   // Set preferred size for the panel
//   setPreferredSize(new Dimension(800, 600))
  
//   // Calculate min and max values for scaling
//   private val maxPrice = data.map(d => max(d.high, max(d.open, d.close))).max
//   private val minPrice = data.map(d => min(d.low, min(d.open, d.close))).min
//   private val maxVolume = data.map(_.volume).max
  
//   override def paintComponent(g: Graphics): Unit = {
//     super.paintComponent(g)
//     val g2 = g.asInstanceOf[Graphics2D]
    
//     // Enable anti-aliasing
//     g2.setRenderingHint(
//       RenderingHints.KEY_ANTIALIASING,
//       RenderingHints.VALUE_ANTIALIAS_ON
//     )
    
//     // Draw background
//     g2.setColor(Color.WHITE)
//     g2.fillRect(0, 0, getWidth, getHeight)
    
//     // Calculate scaling factors
//     val priceRange = maxPrice - minPrice
//     val chartHeight = getHeight - 2 * padding
//     val chartWidth = getWidth - 2 * padding
//     val volumeHeight = chartHeight * 0.2 // 20% of chart height for volume
//     val candleHeight = chartHeight * 0.8 // 80% of chart height for candles
    
//     // Draw price grid and labels
//     g2.setColor(Color.LIGHT_GRAY)
//     for (i <- 0 to 5) {
//       val y = padding + (i * candleHeight / 5).toInt
//       g2.drawLine(padding, y, getWidth - padding, y)
//       val price = maxPrice - (i * priceRange / 5)
//       g2.drawString(f"$price%.3f", 5, y + 5)
//     }
    
//     // Draw candles and volume bars
//     data.zipWithIndex.foreach { case (candle, i) =>
//       val x = padding + i * (candleWidth + candleSpacing)
      
//       // Draw volume bar
//       val volumeBarHeight = (candle.volume * volumeHeight / maxVolume).toInt
//       val volumeY = getHeight - padding - volumeBarHeight
//       g2.setColor(Color.GRAY)
//       g2.fillRect(x, volumeY, candleWidth, volumeBarHeight)
      
//       // Calculate candle coordinates
//       val high = padding + ((maxPrice - candle.high) * candleHeight / priceRange).toInt
//       val low = padding + ((maxPrice - candle.low) * candleHeight / priceRange).toInt
//       val open = padding + ((maxPrice - candle.open) * candleHeight / priceRange).toInt
//       val close = padding + ((maxPrice - candle.close) * candleHeight / priceRange).toInt
      
//       // Draw candle
//       g2.setColor(if (candle.close > candle.open) Color.GREEN else Color.RED)
//       g2.setStroke(new BasicStroke(1))
      
//       // Draw wick
//       g2.drawLine(x + candleWidth/2, high, x + candleWidth/2, low)
      
//       // Draw body
//       val bodyTop = min(open, close)
//       val bodyBottom = max(open, close)
//       g2.fillRect(x, bodyTop, candleWidth, bodyBottom - bodyTop)
      
//       // Draw time labels for every 5th candle
//       if (i % 5 == 0) {
//         g2.setColor(Color.BLACK)
//         g2.drawString(
//           candle.timestamp.toString.take(16), 
//           x, 
//           getHeight - padding/2
//         )
//       }
//     }
//     g2.setColor(Color.ORANGE)
//     g2.setStroke(new BasicStroke(2))
    
//     anomalies.foreach { anomaly =>
//       val index = data.indexOf(anomaly.data)
//       if (index >= 0) {
//         val x = padding + index * (candleWidth + candleSpacing)
        
//         // Draw highlight circle around anomalous candle
//         g2.drawOval(
//           x - 5, 
//           padding + ((maxPrice - anomaly.data.high) * candleHeight / priceRange).toInt - 5,
//           candleWidth + 10,
//           ((anomaly.data.high - anomaly.data.low) * candleHeight / priceRange).toInt + 10
//         )
        
//         // Draw anomaly type
//         g2.drawString(
//           anomaly.anomalyTypes.mkString(","),
//           x,
//           padding - 5
//         )
//       }
//     }
//   }
// }

// object ForexAnalysisApp {
//   val detector = new ForexAnomalyDetector()
//   val filePath = "forex_data.csv"
  
//   // try {
//   //   val reader = new CSVReader(new FileReader(filePath), '\t')
//   //   val allRows = reader.readAll().asScala.toList.tail // Skip header
//   //   val data = detector.parseData(allRows)
//   //   val anomalies = detector.detectAnomalies(filePath)
    
//   //   // Create and show visualization
//   //   val frame = new JFrame("USDJPY OHLC Chart with Anomalies")
//   //   frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
//   //   frame.add(new OHLCChart(data, anomalies))
//   //   frame.pack()
//   //   frame.setLocationRelativeTo(null)
//   //   frame.setVisible(true)
    
//   // } catch {
//   //   case e: Exception =>
//   //     println(s"Error: ${e.getMessage}")
//   //     e.printStackTrace()
//   // }
// }
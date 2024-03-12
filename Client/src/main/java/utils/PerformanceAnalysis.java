package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerformanceAnalysis {

  public static void main(String[] args) {
    // Perform analysis
    Map<String, Long> analysisResults = analyzePerformance();
    writeAnalysisResultsToCsv(analysisResults);
  }
  private static Map<String, Long> analyzePerformance() {
    try {
      List<String> lines = Files.readAllLines(Path.of("latencies_lb.csv"));
      long firstRequestStartTime = Long.parseLong(lines.get(1).split(",")[0]);
      String[] lastLine = lines.get(lines.size() - 1).split(",");
      long lastRequestStartTime = Long.parseLong(lastLine[0]);
      long lastRequestLatency = Long.parseLong(lastLine[2]); // Assuming latency is in milliseconds

      List<Long> latencies = lines.stream().skip(1) // Skip the header
          .map(line -> Long.parseLong(line.split(",")[2])) // Extract latency
          .collect(Collectors.toList());

      latencies.sort(Comparator.naturalOrder()); // Make sure latencies are sorted for percentile calculation

      DoubleSummaryStatistics stats = latencies.stream()
          .mapToDouble(Long::doubleValue)
          .summaryStatistics();

      long mean = (long) stats.getAverage();
      long median = latencies.get(latencies.size() / 2 - (latencies.size() % 2 == 0 ? 1 : 0));
      long p99 = latencies.get((int) (latencies.size() * 0.99) - 1);
      long min = (long) stats.getMin();
      long max = (long) stats.getMax();
      long estimatedTestEndTime = lastRequestStartTime + lastRequestLatency;
      long testDurationMillis = estimatedTestEndTime - firstRequestStartTime;
      double testDurationSeconds = testDurationMillis / 1000.0;
      double throughput = latencies.size() / testDurationSeconds; // Calculate throughput

      System.out.println("Mean response time: " + mean + "ms");
      System.out.println("Median response time: " + median + "ms");
      System.out.println("p99 response time: " + p99 + "ms");
      System.out.println("Min response time: " + min + "ms");
      System.out.println("Max response time: " + max + "ms");
      System.out.println("Throughput: " + throughput + " requests per second");
      // Create analysis result lines
      Map<String, Long> analysisResults = new HashMap<>();
      analysisResults.put("Mean", mean);
      analysisResults.put("Median", median);
      analysisResults.put("p99", p99);
      analysisResults.put("Min", min);
      analysisResults.put("Max", max);

      // Add throughput as a separate entry
      analysisResults.put("Throughput", (long) throughput);
      analysisResults.put("TotalRequests", Long.valueOf(latencies.size()));

      return analysisResults;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeAnalysisResultsToCsv(Map<String, Long> analysisResults) {
    String csvFilePath = "performance_results.csv";

    try (FileWriter writer = new FileWriter(csvFilePath, true)) {
      // Write headers if the file is empty
      if (!Files.exists(Paths.get(csvFilePath)) || Files.size(Paths.get(csvFilePath)) == 0) {
        writer.append("TotalRequests").append(",");
        writer.append("Throughput").append(",");
        writer.append("Min").append(",");
        writer.append("Max").append(",");
        writer.append("Median").append(",");
        writer.append("p99").append(",");
        writer.append("Mean").append("\n");
      }

      // Write data
      writer.append(String.valueOf(analysisResults.get("TotalRequests"))).append(",");
      writer.append(String.valueOf(analysisResults.get("Throughput"))).append(",");
      writer.append(String.valueOf(analysisResults.get("Min"))).append(",");
      writer.append(String.valueOf(analysisResults.get("Max"))).append(",");
      writer.append(String.valueOf(analysisResults.get("Median"))).append(",");
      writer.append(String.valueOf(analysisResults.get("p99"))).append(",");
      writer.append(String.valueOf(analysisResults.get("Mean"))).append("\n");

      System.out.println("Analysis results written to " + csvFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeAnalysisToCSV(Map<String, Long> analysisResults) {
    try (FileWriter writer = new FileWriter("analysis_records.csv")) {
      // Write header line
      writer.write("Metric,Value\n");

      // Write each analysis metric to a separate row
      for (Map.Entry<String, Long> entry : analysisResults.entrySet()) {
        writer.write(entry.getKey() + "," + entry.getValue() + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}

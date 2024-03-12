package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvLogger {
  private static final String LATENCY_FILE_PATH = "latencies_single.csv";
  private static final String THROUGHPUT_FILE_PATH = "throughput_single.csv"; // Corrected file name

  private static BufferedWriter latencyWriter;
  private static BufferedWriter throughputWriter;

  public static synchronized void initializeCsv() throws IOException {
    try {
      // Latency file
      Files.write(Paths.get(LATENCY_FILE_PATH), "StartTime,RequestType,Latency,ResponseCode\n".getBytes(), StandardOpenOption.CREATE);
      latencyWriter = new BufferedWriter(new FileWriter(LATENCY_FILE_PATH, true));

      // Throughput file
      Files.write(Paths.get(THROUGHPUT_FILE_PATH), "Timestamp,Throughput\n".getBytes(), StandardOpenOption.CREATE);
      throughputWriter = new BufferedWriter(new FileWriter(THROUGHPUT_FILE_PATH, true));

      System.out.println("CSV initialized successfully.");
    } catch (IOException e) {
      System.err.println("Failed to initialize CSV: " + e.getMessage());
      throw e;
    }
  }

  public static synchronized void resetCSVFile() {
    try {
      // Reset latency file
      BufferedWriter writer = new BufferedWriter(new FileWriter(LATENCY_FILE_PATH));
      writer.write("startTime,requestType,latency,responseCode\n");
      // Reset throughput file
      BufferedWriter writer2 = new BufferedWriter(new FileWriter(THROUGHPUT_FILE_PATH));
      writer2.write("startTime,requestType,latency,responseCode\n");

      System.out.println("CSV files reset successfully.");
    } catch (IOException e) {
      System.err.println("Error resetting the CSV files: " + e.getMessage());
    }
  }

  public static synchronized void logLatency(String dataRow) {
    try {
      latencyWriter.write(dataRow);
      latencyWriter.flush();
    } catch (IOException e) {
      System.err.println("Error writing to latency CSV file: " + e.getMessage());
    }
  }

  public static synchronized void logThroughput(String dataRow) {
    try {
      throughputWriter.write(dataRow);
      throughputWriter.flush();
    } catch (IOException e) {
      System.err.println("Error writing to throughput CSV file: " + e.getMessage());
    }
  }

  public static String formatLatencyRow(long startTime, String requestType, long latency,
      int responseCode) {
    return startTime + "," + requestType + "," + latency + "," + responseCode + "\n";
  }

  public static String formatThroughputRow(long timestamp, double throughput) {
    return timestamp + "," + throughput + "\n";
  }

  public static synchronized void closeCsv() throws IOException {
    if (latencyWriter != null) {
      latencyWriter.close();
    }
    if (throughputWriter != null) {
      throughputWriter.close();
    }
  }
}

package org.maven.proj.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvLogger {
  private static final String CSV_FILE_PATH = "latencies.csv";
  private static BufferedWriter bufferedWriter;

  public static synchronized void initializeCsv() throws IOException {
    Files.write(Paths.get(CSV_FILE_PATH), "StartTime,RequestType,Latency,ResponseCode\n".getBytes(), StandardOpenOption.CREATE);
    bufferedWriter = new BufferedWriter(new FileWriter(CSV_FILE_PATH, true));
  }

  public static synchronized void logToCsv(String dataRow) {
    try {
      bufferedWriter.write(dataRow);
      bufferedWriter.flush(); // Ensure data is written immediately
    } catch (IOException e) {
      System.err.println("Error writing to CSV file: " + e.getMessage());
    }
  }

  public static void resetCSVFile() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE_PATH))) {
      writer.write("startTime,requestType,latency,responseCode\n"); // Optionally write header on reset
    } catch (IOException e) {
      System.err.println("Error resetting the CSV file: " + e.getMessage());
    }
  }

  private void appendDataToCsv(long startTime, String requestType, long latency, int responseCode) {
    String filePath = "latencies.csv";
    Path path = Paths.get(filePath);
    String dataRow = startTime + "," + requestType + "," + latency + "," + responseCode + "\n";
    // Append data row to the CSV
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
      writer.write(dataRow);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void closeCsv() throws IOException {
    if (bufferedWriter != null) {
      bufferedWriter.close();
    }
  }
}

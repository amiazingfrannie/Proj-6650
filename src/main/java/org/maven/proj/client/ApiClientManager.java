package org.maven.proj.client;

import io.swagger.client.ApiCallback;
import java.io.IOException;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.maven.proj.utils.CsvLogger;

public class ApiClientManager {

  private SkiersApi skiersApi;
  private AtomicInteger successfulRequests = new AtomicInteger(0);
  private AtomicInteger unsuccessfulRequests = new AtomicInteger(0);

  public ApiClientManager(String basePath) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basePath);
    this.skiersApi = new SkiersApi(apiClient);
  }

  /**
   * for testing single thread request sending
   * @param liftRide
   * @param resortID
   * @param seasonID
   * @param dayID
   * @param skierID
   */
  public void sendLiftRide(LiftRide liftRide, Integer resortID, String seasonID, String dayID, Integer skierID) {
    try {
      skiersApi.writeNewLiftRide(liftRide, resortID, seasonID, dayID, skierID); // Hypothetical synchronous method
      System.out.println("Request successfully sent");
      successfulRequests.incrementAndGet();
    } catch (ApiException e) {
      System.out.println("Request failed sent");
      unsuccessfulRequests.incrementAndGet();
    }
  }

  /**
   * for multi threads requests sending
   * have 5 attempts of retry
   * @param liftRide
   * @param resortID
   * @param seasonID
   * @param dayID
   * @param skierID
   * @param latch
   * @param requestStartTime
   */
  public void sendLiftRideWithRetry(LiftRide liftRide, Integer resortID, String seasonID, String dayID, Integer skierID, CountDownLatch latch, long requestStartTime) {
    final int maxRetries = 5;
    boolean isRequestSuccessful = false;
    ApiException lastException = null;

    for (int i = 0; i <= maxRetries; i++) {
      try {
        if (i > 0) {
          int backoffTime = i * 1000; // Simple linear backoff strategy
          System.err.println("Retry attempt " + i + " for lift ride. Backing off for " + backoffTime + "ms.");
          Thread.sleep(backoffTime);
        }
        skiersApi.writeNewLiftRide(liftRide, resortID, seasonID, dayID, skierID);
        isRequestSuccessful = true;
        System.out.println("Request successful on attempt " + i);
        break; // Break out of the loop if the request is successful
      } catch (ApiException e) {
        lastException = e;
        System.err.println("Attempt " + i + " failed with status code: " + e.getCode() + " and message: " + e.getMessage());
      } catch (InterruptedException ie) {
        System.err.println("Thread interrupted during backoff wait. Attempt: " + i);
        Thread.currentThread().interrupt(); // Preserve interrupt status
        break; // Exit retry loop if the thread is interrupted
      }
    }

    long requestEndTime = System.currentTimeMillis();
    long latency = requestEndTime - requestStartTime;
    String dataRow = formatCsvRow(requestStartTime, "POST", latency, isRequestSuccessful ? 200 : (lastException != null ? lastException.getCode() : 201));
    CsvLogger.logToCsv(dataRow);

    if (!isRequestSuccessful) {
      System.err.println("Failed to send lift ride after " + maxRetries + " attempts. Last known error: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
      unsuccessfulRequests.incrementAndGet();
    } else {
      successfulRequests.incrementAndGet();
    }

    latch.countDown();
  }

  /**
   * This is calling skiersApi.writeNewLiftRideAsync, somehow it's slower than sendLiftRide
   * need to explore why, not calling for now
   * @param liftRide
   * @param resortID
   * @param seasonID
   * @param dayID
   * @param skierID
   * @param latch
   * @param requestStartTime
   * @throws ApiException
   */
  public void sendLiftRideMultiThreads(LiftRide liftRide, Integer resortID, String seasonID, String dayID, Integer skierID, CountDownLatch latch, long requestStartTime)
      throws ApiException {
    sendLiftRideAsync(liftRide, resortID, seasonID, dayID, skierID, latch, requestStartTime, 0);
  }

  private void sendLiftRideAsync(LiftRide liftRide, Integer resortID, String seasonID, String dayID, Integer skierID, CountDownLatch latch, long requestStartTime, int attempt)
      throws ApiException {
      final int maxRetries = 5;
      skiersApi.writeNewLiftRideAsync(liftRide, resortID, seasonID, dayID, skierID, new ApiCallback<Void>() {
      @Override
      public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
        if (attempt < maxRetries) {
          // Retry the request
          System.err.println("Attempt " + (attempt + 1) + " failed, retrying...");
          try {
            sendLiftRideAsync(liftRide, resortID, seasonID, dayID, skierID, latch, requestStartTime, attempt + 1);
          } catch (ApiException ex) {
            throw new RuntimeException(ex);
          }
        } else {
          // Handle the final failure
          long requestEndTime = System.currentTimeMillis();
          long latency = requestEndTime - requestStartTime;
          String dataRow = formatCsvRow(requestStartTime, "POST", latency, statusCode);
          CsvLogger.logToCsv(dataRow);
          System.err.println("Failed to send lift ride after " + maxRetries + " attempts: " + e.getMessage() + " Status code: " + statusCode);
          if (responseHeaders != null) {
            responseHeaders.forEach((key, value) -> System.err.println(key + ": " + value));
          }
          unsuccessfulRequests.incrementAndGet();
          latch.countDown();
        }
      }

      @Override
      public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
        long requestEndTime = System.currentTimeMillis();
        long latency = requestEndTime - requestStartTime;
        String dataRow = formatCsvRow(requestStartTime, "POST", latency, statusCode);
        CsvLogger.logToCsv(dataRow);
        System.out.println("Lift ride successfully sent.");
        successfulRequests.incrementAndGet();
        latch.countDown(); // Decrement the latch to signal task completion
      }

      @Override
      public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      }

      @Override
      public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
      }
    });
  }

  // Helper method to format the CSV row
  private String formatCsvRow(long startTime, String requestType, long latency, int responseCode) {
    return startTime + "," + requestType + "," + latency + "," + responseCode + "\n";
  }


  // Getters for successful and unsuccessful request counts
  public int getSuccessfulRequests() {
    return successfulRequests.get();
  }

  public int getUnsuccessfulRequests() {
    return unsuccessfulRequests.get();
  }
}


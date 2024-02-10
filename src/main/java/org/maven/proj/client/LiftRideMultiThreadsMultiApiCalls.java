package org.maven.proj.client;

import static org.maven.proj.utils.CsvLogger.initializeCsv;
import static org.maven.proj.utils.CsvLogger.resetCSVFile;

import io.swagger.client.model.LiftRide;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is to test the performance of having one api call for each thread
 * Might be good for heavy concurrency
 * For HW1, use LiftRideMultiThreadsSharedApi class
 */
public class LiftRideMultiThreadsMultiApiCalls {

  final static int TOTAL_THREADS = 32;
  final static int REQUESTS_PER_THREAD = 1000;
  final static int TOTAL_REQUESTS = TOTAL_THREADS * REQUESTS_PER_THREAD;
  private static final int API_CLIENT_MANAGER_POOL_SIZE = TOTAL_THREADS;
  private static final ApiClientManager[] apiClientManagerPool = new ApiClientManager[API_CLIENT_MANAGER_POOL_SIZE];

  static {
    for (int i = 0; i < API_CLIENT_MANAGER_POOL_SIZE; i++) {
      try {
        apiClientManagerPool[i] = new ApiClientManager("http://localhost:8080/Proj_6650_war_exploded");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    resetCSVFile();
    initializeCsv();
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREADS);
    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < TOTAL_THREADS; i++) {
      final int apiClientIndex = i % API_CLIENT_MANAGER_POOL_SIZE;
      ApiClientManager threadApiClientManager = apiClientManagerPool[apiClientIndex];
      executor.submit(() -> {
        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
          CustomizedLiftRide event = LiftRideGenerator.generateRandomLiftRide();
          LiftRide liftRide = new LiftRide();
          liftRide.setTime(event.getTime());
          liftRide.setLiftID(event.getLiftID());
          long requestStartTime = System.currentTimeMillis();
          threadApiClientManager.sendLiftRideWithRetry(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID(), latch, requestStartTime);
        }
      });
    }

    executor.shutdown();
    try {
      latch.await(); // Wait for all tasks to complete
      // After executor.shutdown() and latch.await(), get metrics
      int totalSuccess = 0;
      int totalFailures = 0;
      for (ApiClientManager manager : apiClientManagerPool) {
        totalSuccess += manager.getSuccessfulRequests();
        totalFailures += manager.getUnsuccessfulRequests();
      }
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      double seconds = totalTime / 1000.0;
      System.out.println("All lift ride tasks completed.");
      System.out.println("Number of threads: " + TOTAL_THREADS);
      System.out.println("Number of requests per thread: " + REQUESTS_PER_THREAD);
      System.out.println("Number of successful requests: " + totalSuccess);
      System.out.println("Number of unsuccessful requests: " + totalFailures);
      System.out.println("Total run time: " + seconds + " seconds");
      System.out.println("Total throughput: " + (TOTAL_REQUESTS / seconds) + " requests per second");
      System.out.println("Response time: " + (seconds / TOTAL_REQUESTS) + " second per request");
      System.out.println("Estimated throughput: " + 1/(seconds / TOTAL_REQUESTS) + " second per request");
      System.exit(0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Interrupted before completion: " + e.getMessage());
    }
  }

}

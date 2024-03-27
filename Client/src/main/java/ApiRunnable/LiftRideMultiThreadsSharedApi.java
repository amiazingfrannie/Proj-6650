package ApiRunnable;


import static utils.CsvLogger.initializeCsv;
import static utils.CsvLogger.resetCSVFile;

import ApiManager.ApiClientManager;
import io.swagger.client.model.LiftRide;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import liftRideCustomized.LiftRideGenerator;
import liftRideCustomized.CustomizedLiftRide;


/*
class for assignment 1, pls ignore
 */
public class LiftRideMultiThreadsSharedApi {

  final static int TOTAL_THREADS = 200;
  final static int REQUESTS_PER_THREAD = 100;
  final static int TOTAL_REQUESTS = TOTAL_THREADS * REQUESTS_PER_THREAD;

  public static void main(String[] args) throws IOException {
    resetCSVFile();
    initializeCsv();
    ApiClientManager apiClientManager = new ApiClientManager("http://34.227.24.115:8080/Proj-6650_war");
//    ApiManager.ApiClientManager apiClientManager = new ApiManager.ApiClientManager("http://localhost:8080/Proj_6650_war_exploded");
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREADS);
    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < TOTAL_THREADS; i++) {
      System.out.println("[C] thread: " + i);
      executor.submit(() -> {
        for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
          CustomizedLiftRide event = LiftRideGenerator.generateRandomCustomizedLiftRide();
          LiftRide liftRide = new LiftRide();
          liftRide.setTime(event.getTime());
          liftRide.setLiftID(event.getLiftID());
//          long requestStartTime = System.currentTimeMillis();
//          apiClientManager.sendLiftRideWithRetry(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID(), latch, requestStartTime);
          System.out.println("[C] message: " + j);
          apiClientManager.sendLiftRideWithRetry(event,latch);
        }
      });
    }

    executor.shutdown();
    try {
      latch.await(); // Wait for all tasks to complete
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      double seconds = totalTime / 1000.0;
      System.out.println("All lift ride tasks completed.");
      System.out.println("Number of threads: " + TOTAL_THREADS);
      System.out.println("Number of requests per thread: " + REQUESTS_PER_THREAD);
      System.out.println("Number of successful requests: " + apiClientManager.getSuccessfulRequests());
      System.out.println("Number of unsuccessful requests: " + apiClientManager.getUnsuccessfulRequests());
      System.out.println("Total run time: " + seconds + " seconds");
      System.out.println("Total throughput: " + (TOTAL_REQUESTS / seconds) + " requests per second");
      System.out.println("Response time: " + (totalTime / TOTAL_REQUESTS) + " ms per request");
//      System.out.println("Estimated throughput: " + 1/(seconds / TOTAL_REQUESTS) + " second per request");
      System.exit(0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Interrupted before completion: " + e.getMessage());
    }
  }

}

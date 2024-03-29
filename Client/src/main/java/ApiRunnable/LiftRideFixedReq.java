package ApiRunnable;

import static utils.CsvLogger.formatThroughputRow;
import static utils.CsvLogger.initializeCsv;
import static utils.CsvLogger.resetCSVFile;

import ApiManager.ApiClientManager;
import liftRideCustomized.LiftRideGenerator;
import java.io.IOException;
import java.util.concurrent.*;
import utils.CsvLogger;

/**
 * for hw3, implemented this class
 * to utilize dynamic thread
 */
public class LiftRideFixedReq {
  private static final int TOTAL_REQUESTS = 200_000;

  public static void main(String[] args) throws IOException {
    resetCSVFile();
    initializeCsv();
    int minThreads = 32;
    int maxThreads = 200;
//     ApiClientManager apiClientManager = new ApiClientManager("http://servlet-410525632.us-east-1.elb.amazonaws.com:8080/Proj-6650_war");
    ApiClientManager apiClientManager = new ApiClientManager("http://54.224.141.103:8080/Proj-6650_war");
//    ApiClientManager apiClientManager = new ApiClientManager("http://localhost:8080/Proj_6650_war_exploded");
    CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        minThreads,
        maxThreads,
        60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(5000),
        new ThreadPoolExecutor.CallerRunsPolicy());

    ScheduledExecutorService throughputLogger = Executors.newSingleThreadScheduledExecutor();
    long startTime = System.currentTimeMillis();

    // Schedule the throughput logging to run every 10 seconds
    throughputLogger.scheduleAtFixedRate(() -> {
      long currentTime = System.currentTimeMillis();
      double secondsElapsed = (currentTime - startTime) / 1000.0;
      int requestsProcessed = apiClientManager.getSuccessfulRequests();
      double throughput = requestsProcessed / secondsElapsed;
      String dataRow = formatThroughputRow(currentTime, throughput);
      CsvLogger.logThroughput(dataRow);
    }, 0, 10, TimeUnit.SECONDS);


    for (int i = 0; i < TOTAL_REQUESTS; i++) {
      executor.execute(() -> {
        apiClientManager.sendLiftRideWithRetry(LiftRideGenerator.generateRandomCustomizedLiftRide(), latch);
      });
    }

    shutdownAndAwaitTermination(executor);
    throughputLogger.shutdown(); // Shutdown the throughput logger
    try {
      if (!throughputLogger.awaitTermination(60, TimeUnit.SECONDS)) {
        throughputLogger.shutdownNow();
      }
    } catch (InterruptedException e) {
      throughputLogger.shutdownNow();
      Thread.currentThread().interrupt();
    }

    long endTime = System.currentTimeMillis();
    printFinalStatistics(startTime, endTime, apiClientManager);
  }

  private static void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
          System.err.println("Executor did not terminate");
        }
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private static void printFinalStatistics(long startTime, long endTime, ApiClientManager apiClientManager) {
    double seconds = (endTime - startTime) / 1000.0;
    System.out.println("All lift ride tasks completed.");
    System.out.println("Number of successful requests: " + apiClientManager.getSuccessfulRequests());
    System.out.println("Number of unsuccessful requests: " + apiClientManager.getUnsuccessfulRequests());
    System.out.println("Total run time: " + seconds + " seconds");
    System.out.println("Total throughput: " + (TOTAL_REQUESTS / seconds) + " requests per second");
    System.out.println("Response time: " + ((endTime - startTime) / TOTAL_REQUESTS) + " ms per request");
  }
}

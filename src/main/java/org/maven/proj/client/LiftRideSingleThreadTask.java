package org.maven.proj.client;

import io.swagger.client.model.LiftRide;
import java.io.IOException;

public class LiftRideSingleThreadTask {

  final static int TOTAL_THREADS = 1;
  final static int REQUESTS_PER_THREAD = 32000;
  final static int TOTAL_REQUESTS = TOTAL_THREADS * REQUESTS_PER_THREAD;

  public static void main(String[] args) throws IOException {

    ApiClientManager apiClientManager = new ApiClientManager("http://54.209.19.73:8080/Proj-6650_war");
    long startTime = System.currentTimeMillis();

    for (int j = 0; j < TOTAL_REQUESTS; j++) {
      CustomizedLiftRide event = LiftRideGenerator.generateRandomLiftRide();
      LiftRide liftRide = new LiftRide();
      liftRide.setTime(event.getTime());
      liftRide.setLiftID(event.getLiftID());
      apiClientManager.sendLiftRide(liftRide, event.getResortID(), event.getSeasonID(),
          event.getDayID(), event.getSkierID());
    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    double seconds = totalTime / 1000.0;
    System.out.println("Number of successful requests: " + apiClientManager.getSuccessfulRequests());
    System.out.println("Number of unsuccessful requests: " + apiClientManager.getUnsuccessfulRequests());
    System.out.println("All lift ride tasks completed.");
    System.out.println("Total run time: " + seconds + " seconds");
    System.out.println("Total throughput: " + (TOTAL_REQUESTS / seconds) + " requests per second");
    System.out.println("Response time: " + (seconds / TOTAL_REQUESTS) + " second per request");
    System.out.println("Estimated throughput: " + 1/(seconds / TOTAL_REQUESTS) + " second per request");
//      System.exit(0);
  }
}

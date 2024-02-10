package org.maven.proj.client;

import java.util.concurrent.ThreadLocalRandom;
import org.maven.proj.client.CustomizedLiftRide;

public class LiftRideGenerator {

  final static String SEASON_ID = "2024";

  public static CustomizedLiftRide generateRandomLiftRide() {
    int skierID = ThreadLocalRandom.current().nextInt(1, 100001);
    int resortID = ThreadLocalRandom.current().nextInt(1, 11);
    int liftID = ThreadLocalRandom.current().nextInt(1, 41);
    String seasonID = SEASON_ID;
    String dayID = "1";
    int time = ThreadLocalRandom.current().nextInt(1, 361);

    return new CustomizedLiftRide(skierID, resortID, liftID, seasonID, dayID, time);
  }
}


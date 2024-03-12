package liftRideCustomized;

import io.swagger.client.model.LiftRide;
import java.util.concurrent.ThreadLocalRandom;

public class LiftRideGenerator {

  final static String SEASON_ID = "2024";

  public static CustomizedLiftRide generateRandomCustomizedLiftRide() {
    int skierID = ThreadLocalRandom.current().nextInt(1, 100001);
    int resortID = ThreadLocalRandom.current().nextInt(1, 11);
    int liftID = ThreadLocalRandom.current().nextInt(1, 41);
    String seasonID = SEASON_ID;
    String dayID = "1";
    int time = ThreadLocalRandom.current().nextInt(1, 361);

    return new CustomizedLiftRide(skierID, resortID, liftID, seasonID, dayID, time);
  }

  public static LiftRide generateRandomLiftRide() {
    LiftRide liftride = new LiftRide();
    int liftID = ThreadLocalRandom.current().nextInt(1, 41);
    int time = ThreadLocalRandom.current().nextInt(1, 361);
    liftride.setLiftID(liftID);
    liftride.setTime(time);
    return liftride;
  }
}


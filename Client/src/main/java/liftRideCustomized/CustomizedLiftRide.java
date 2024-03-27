package liftRideCustomized;

public class CustomizedLiftRide {

  private int skierID;
  private int resortID;
  private int liftID;
  private String seasonID;
  private String dayID;
  private int time;

  public CustomizedLiftRide(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
    this.skierID = skierID;
    this.resortID = resortID;
    this.liftID = liftID;
    this.seasonID = seasonID;
    this.dayID = dayID;
    this.time = time;
  }

  public int getSkierID() {
    return skierID;
  }

  public void setSkierID(int skierID) {
    this.skierID = skierID;
  }

  public int getResortID() {
    return resortID;
  }

  public void setResortID(int resortID) {
    this.resortID = resortID;
  }

  public int getLiftID() {
    return liftID;
  }

  public void setLiftID(int liftID) {
    this.liftID = liftID;
  }

  public String getSeasonID() {
    return this.seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public String getDayID() {
    return this.dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return "LiftRide{" +
        "skierID=" + skierID +
        ", resortID=" + resortID +
        ", liftID=" + liftID +
        ", seasonID='" + seasonID + '\'' +
        ", dayID=" + dayID +
        ", time=" + time +
        '}';
  }
}

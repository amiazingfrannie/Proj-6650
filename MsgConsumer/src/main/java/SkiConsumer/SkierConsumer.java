package SkiConsumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import liftRideCustomized.CustomizedLiftRide;
import redis.clients.jedis.Jedis;

public class SkierConsumer {

  private static final ConnectionFactory factory = new ConnectionFactory();
  private static Connection connection;
  private static final String QUEUE_NAME = "proj_queue";
  private static final ConcurrentHashMap<Integer, Integer> liftRidesMap = new ConcurrentHashMap<>();
  private static final Integer TOTAL_THREAD = 10;

  static {
    // These should be externalized in a configuration file or environment variables
    factory.setUsername("user1");
    factory.setPassword("password");
    factory.setHost("18.207.126.125");
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    Channel channel = null;
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREAD); // Create a thread pool with 10 worker threads

    try {
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        executor.submit(() -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//          System.out.println(message);

          Gson gson = new Gson();
          CustomizedLiftRide liftRide = gson.fromJson(message, CustomizedLiftRide.class);
          System.out.println(" [x] Received '" + liftRide + "'");

          try (Jedis jedis = new Jedis("18.207.168.132",
              6379)) { // Use try-with-resources for auto-closing
            jedis.auth("password");
            // 1. Add the day to the skier's set of days skied this season
            String daysKey = String.format("skier:%d:seasons:%s:days", liftRide.getSkierID(),
                liftRide.getSeasonID());
            jedis.sadd(daysKey, liftRide.getDayID());

            // 2. Increment the vertical total for the skier for the day
            String verticalKey = String.format("skier:%d:seasons:%s:days:%s:vertical",
                liftRide.getSkierID(), liftRide.getSeasonID(), liftRide.getDayID());
            // Safely increment vertical, ensure the key is initialized properly
            if (jedis.exists(verticalKey) && !jedis.type(verticalKey).equals("string")) {
              System.err.println("Expected 'string' type for key: " + verticalKey + ", but found: " + jedis.type(verticalKey));
              // Handle error: key exists but is not a string.
              // This might involve logging the error, skipping the operation, or even cleaning up data if feasible.
            } else {
              // Proceed with the operation assuming the key is correctly set or doesn't exist
              jedis.incrBy(verticalKey, liftRide.getLiftID() * 10); // Assuming vertical = liftID * 10
            }

            // 3. Record the lift ride
            String liftsKey = String.format("skier:%d:seasons:%s:days:%s:lifts",
                liftRide.getSkierID(), liftRide.getSeasonID(), liftRide.getDayID());
            jedis.rpush(liftsKey, String.valueOf(liftRide.getLiftID()));

            // 4. Add the skier to the set of unique skiers for the resort for the day
            String resortSkiersKey = String.format("resort:%d:days:%s:skiers",
                liftRide.getResortID(), liftRide.getDayID());
            jedis.sadd(resortSkiersKey, String.valueOf(liftRide.getSkierID()));
          } catch (Exception e) {
            System.err.println("Redis operation failed: " + e.getMessage());
          }
        });
      };
      channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
      Thread.currentThread().join();

    } catch (IOException | TimeoutException | InterruptedException e) {
      e.printStackTrace();
      System.out.println(" [*] Error while connecting or receiving");
    } finally {
      // Add shutdown hook to close resources
      final Channel finalChannel = channel;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          if (finalChannel != null && finalChannel.isOpen()) {
            finalChannel.close();
          }
          if (connection != null && connection.isOpen()) {
            connection.close();
          }
        } catch (IOException | TimeoutException e) {
          e.printStackTrace();
        }
        executor.shutdown();
        try {
          if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
          }
        } catch (InterruptedException e) {
          executor.shutdownNow();
        }
      }));
    }
  }
}

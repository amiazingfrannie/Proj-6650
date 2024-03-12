package SkiConsumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import io.swagger.client.model.LiftRide;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    factory.setHost("54.221.131.110");
  }

  public static void main(String[] args) throws IOException, TimeoutException {
    Channel channel = null;
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREAD); // Create a thread pool with 10 worker threads

    try  {
      connection = factory.newConnection();
      channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);

      // Define a consumer callback to handle incoming messages
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        executor.submit(() -> {
          String message;
          try {
            message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
          } catch (UnsupportedEncodingException e) {
            System.out.println(" [*] Error while receiving ");
            throw new RuntimeException(e);
          }

          // Parse the message (assuming it contains JSON data with liftID and skierID)
          Gson gson = new Gson();
          LiftRide liftRide = gson.fromJson(message, LiftRide.class);
          int time = liftRide.getTime();
          int liftID = liftRide.getLiftID();
          System.out.println(" [x] Processed '" + liftRide + "'");

          // Update the liftRidesMap in a thread-safe manner
          liftRidesMap.put(time, liftID);

        });
      };

      // Start consuming messages from the queue
      channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
      System.out.println(" [*] Message Consumed");
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

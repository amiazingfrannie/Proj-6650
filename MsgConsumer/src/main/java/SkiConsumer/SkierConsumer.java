package SkiConsumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import liftRideCustomized.CustomizedLiftRide;
import redis.clients.jedis.Jedis;

public class SkierConsumer {

  private static final ConnectionFactory factory = new ConnectionFactory();
  private static Connection connection;
  private static Channel channel;
  private static final String QUEUE_NAME = "proj_queue";
  private static final Integer TOTAL_THREAD = 100;
  private static final AtomicInteger messagesStored = new AtomicInteger(0);

  static {
    factory.setUsername("user1");
    factory.setPassword("password");
    factory.setHost("54.82.120.124");
  }

  public static void main(String[] args) {
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREAD);
    try {
      connection = factory.newConnection();
      channel = connection.createChannel(); // Initialized here, within the try block
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);

      // Prefetch count to distribute workload evenly
      int prefetchCount = 10;
      channel.basicQos(prefetchCount);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        executor.submit(() -> {
          try {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            CustomizedLiftRide liftRide = gson.fromJson(message, CustomizedLiftRide.class);

            processMessage(liftRide, channel, delivery.getEnvelope().getDeliveryTag());
          } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
            try {
              channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true); // Consider false + DLX
            } catch (IOException ioException) {
              ioException.printStackTrace();
            }
          }
        });
      };

      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
      Thread.currentThread().join();

    } catch (IOException | TimeoutException | InterruptedException e) {
      e.printStackTrace();
      System.out.println(" [*] Error while connecting or receiving");
    } finally {
      shutdownResources(executor); // Pass only executor to the method
    }
  }

  private static void processMessage(CustomizedLiftRide liftRide, Channel channel, long deliveryTag) throws IOException {
    long startTime = System.currentTimeMillis();
    try (Jedis jedis = RedisPoolManager.getPool().getResource()) {

      var pipeline = jedis.pipelined();

      String daysKey = String.format("skier:%d:seasons:%s:days", liftRide.getSkierID(), liftRide.getSeasonID());
      pipeline.sadd(daysKey, liftRide.getDayID());

      String verticalKey = String.format("skier:%d:seasons:%s:days:%s:vertical", liftRide.getSkierID(), liftRide.getSeasonID(), liftRide.getDayID());
      // Assume the key type check and handling are done correctly here
      pipeline.incrBy(verticalKey, liftRide.getLiftID() * 10);

      String liftsKey = String.format("skier:%d:seasons:%s:days:%s:lifts", liftRide.getSkierID(), liftRide.getSeasonID(), liftRide.getDayID());
      pipeline.rpush(liftsKey, String.valueOf(liftRide.getLiftID()));

      String resortSkiersKey = String.format("resort:%d:days:%s:skiers", liftRide.getResortID(), liftRide.getDayID());
      pipeline.sadd(resortSkiersKey, String.valueOf(liftRide.getSkierID()));

      pipeline.sync(); // Execute all commands in the pipeline

      // Acknowledge the message after successful processing
      channel.basicAck(deliveryTag, false);

      int currentStored = messagesStored.incrementAndGet();
      // Capture the end time for processing
      long endTime = System.currentTimeMillis();

      // Log processing details
      System.out.println(String.format("Thread: %s | Stored Count: %d | Processing Time: %d ms",
          Thread.currentThread().getName(),
          currentStored,
          endTime - startTime));
      // Conditional logging for every 100 messages
      if (currentStored % 100 == 0) {
        System.out.println(currentStored + " messages have been stored successfully.");
      }
    }
  }

  private static void shutdownResources(ExecutorService executor) {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }

    executor.shutdown();
    RedisPoolManager.closePool();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }
}

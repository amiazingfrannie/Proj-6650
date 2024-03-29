package SkiServlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import liftRideCustomized.CustomizedLiftRide;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@WebServlet(name = "SkierServlet", asyncSupported = true)
public class SkierServlet extends HttpServlet {
  private static final List<CustomizedLiftRide> liftRides = new ArrayList<>();
  private static final String QUEUE_NAME = "proj_queue";
  private static final int POOL_SIZE = 10; // Number of channels in the pool
  private static GenericObjectPool<Channel> channelPool;
  private ExecutorService executorService = Executors.newFixedThreadPool(10); // Customize the pool size as needed
  private static final AtomicInteger messagesSent = new AtomicInteger(0);

  @Override
  public void init() throws ServletException {
    super.init();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername("user1");
    factory.setPassword("password");
    factory.setHost("54.82.120.124");

    try {
      Connection connection = factory.newConnection();
      // Setup publisher confirms on a new channel
      try (Channel confirmChannel = connection.createChannel()) {
        setupPublisherConfirms(confirmChannel);
      }
      channelPool = createChannelPool(connection);
      // Declare the queue using a channel from the pool
      try (Channel channel = channelPool.borrowObject()) {
        declareQueue(channel);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (IOException | TimeoutException e) {
      throw new ServletException("Error initializing RabbitMQ connection", e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    final AsyncContext asyncContext = request.startAsync();
    asyncContext.setTimeout(10000); // Adjust timeout as needed

    // Use the executor service to handle request processing asynchronously
    executorService.execute(() -> {
      try {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Gson gson = new Gson();
        CustomizedLiftRide newLiftRide = gson.fromJson(requestBody, CustomizedLiftRide.class);

        if (!CustomizediftrideIsValid(newLiftRide)) {
          sendErrorResponse(asyncContext, HttpServletResponse.SC_BAD_REQUEST, "Invalid LiftRide object.");
          return;
        }

        // Process the message asynchronously
        processMessageAsync(newLiftRide, gson, asyncContext);
      } catch (Exception e) {
        sendErrorResponse(asyncContext, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        e.printStackTrace(); // Log the error appropriately
      }
    });
  }

  private void processMessageAsync(CustomizedLiftRide liftRide, Gson gson, AsyncContext asyncContext) {
    try {
      Channel channel = channelPool.borrowObject();
      try {
        publishToQueue(channel, gson.toJson(liftRide));
        sendSuccessResponse(asyncContext, "Lift ride added successfully.");
      } finally {
        channelPool.returnObject(channel);
      }
    } catch (Exception e) {
      sendErrorResponse(asyncContext, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing request.");
      e.printStackTrace(); // Log the error appropriately
    }
  }

  private void sendErrorResponse(AsyncContext asyncContext, int statusCode, String message) {
    try {
      HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
      response.setStatus(statusCode);
      PrintWriter writer = response.getWriter();
      writer.write("{\"error\": \"" + message + "\"}");
    } catch (IOException e) {
      e.printStackTrace(); // Log this error
    } finally {
      asyncContext.complete();
    }
  }

  private void sendSuccessResponse(AsyncContext asyncContext, String message) {
    try {
      HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
      response.setStatus(HttpServletResponse.SC_CREATED);
      PrintWriter writer = response.getWriter();
      writer.write("{\"message\": \"" + message + "\"}");
    } catch (IOException e) {
      e.printStackTrace(); // Log this error
    } finally {
      asyncContext.complete();
    }
  }

  private GenericObjectPool<Channel> createChannelPool(Connection connection) {
    GenericObjectPoolConfig<Channel> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(POOL_SIZE);
    poolConfig.setMinIdle(1);
    poolConfig.setMaxIdle(POOL_SIZE);
    poolConfig.setBlockWhenExhausted(true);

    return new GenericObjectPool<>(new PooledChannelFactory(connection), poolConfig);
  }

  // Assume this method is called once during application initialization or connection setup
  public void declareQueue(Channel channel) throws IOException {
    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    System.out.println("Queue declared: " + QUEUE_NAME);
  }

//  private void publishToQueue(Channel channel, String message) throws IOException {
//    try {
//      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
//      System.out.println(" [x] Sent '" + message + "' to remote queue.");
//    } catch (IOException e) {
//      System.err.println("Failed to publish message: " + e.getMessage());
//      throw e; // Rethrow or handle as appropriate for your application's error handling policy
//    }
//  }

  private void publishToQueue(Channel channel, String message) throws IOException {
    try {
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
      int count = messagesSent.incrementAndGet();
      System.out.println(" [x] Sent '" + message + "' to remote queue. Total sent: " + count);
    } catch (IOException e) {
      System.err.println("Failed to publish message: " + e.getMessage());
      throw e;
    }
  }

  private void setupPublisherConfirms(Channel channel) throws IOException {
    channel.confirmSelect();
    channel.addConfirmListener((deliveryTag, multiple) -> {
      System.out.println("Message with delivery tag " + deliveryTag + " confirmed.");
    }, (deliveryTag, multiple) -> {
      System.err.println("Warning: Message with delivery tag " + deliveryTag + " failed to send.");
    });
  }


  @Override
  public void destroy() {
    try {
      executorService.shutdown();
      channelPool.close();
    } catch (Exception e) {
      e.printStackTrace(); // Ideally, use a logger
    }
  }

//  private boolean liftrideIsValid(LiftRide liftRide) {
//    return liftRide != null &&
//        liftRide.getLiftID() != null &&
//        liftRide.getTime() != null &&
//        liftRide.getLiftID() > 0 &&
//        liftRide.getTime() > 0;
//  }

  private boolean CustomizediftrideIsValid(CustomizedLiftRide liftRide) {
    return liftRide != null &&
        liftRide.getLiftID() >= 0 &&
        liftRide.getTime() >=0  &&
        liftRide.getLiftID() > 0 &&
        liftRide.getTime() > 0;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();
    out.write("{\"message\": \"GET request received.\"}");

    try {
      synchronized (liftRides) {
        if (liftRides != null && !liftRides.isEmpty()) {
          Gson gson = new Gson();
          String jsonResponse = gson.toJson(liftRides);
          response.setStatus(HttpServletResponse.SC_OK);
          out.write(jsonResponse);
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          out.write("{\"message\": \"No lift rides found.\"}");
        }
      }
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      out.write("{\"error\": \"An error occurred while processing lift rides.\"}");
      // Log the exception
      e.printStackTrace();
    }
  }

}
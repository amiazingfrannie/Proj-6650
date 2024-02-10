package org.maven.proj.server;

import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "SkierServlet")
public class SkierServlet extends HttpServlet {
  // Simulate a database with an in-memory list...Any other data storage?
  private static final List<LiftRide> liftRides = new ArrayList<>();

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter out = response.getWriter();
    out.write("{\"message\": \"POST request received.\"}");

    try {
      // Parse the incoming JSON to a LiftRide object
      String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      System.out.println("request is " + requestBody);
      Gson gson = new Gson();
      LiftRide newLiftRide = gson.fromJson(requestBody, LiftRide.class);
      System.out.println("Lift ride added: " + newLiftRide);

      // Simulate storing the lift ride in the database
      synchronized (liftRides) {
        liftRides.add(newLiftRide);
      }
      response.setStatus(HttpServletResponse.SC_CREATED);
      response.getWriter().write("{\"message\": \"Lift ride added successfully.\"}");
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("{\"error\": \"Bad request due to invalid format.\"}");
    }
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

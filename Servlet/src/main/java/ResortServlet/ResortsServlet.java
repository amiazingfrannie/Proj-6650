package ResortServlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;

@WebServlet(name = "ResortsServlet")
public class ResortsServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("application/json");
    String skierId = request.getParameter("skierId");
    String seasonId = request.getParameter("seasonId");
    String dayId = request.getParameter("dayId");
    String resortId = request.getParameter("resortId");

    // Adding initial checks for null parameters
    if (skierId == null || seasonId == null || dayId == null || resortId == null) {
      try {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("{\"error\":\"Missing required parameters.\"}");
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    try (Jedis jedis = new Jedis("18.207.168.132", 6379)) {
      jedis.auth("password");

      Set<String> daysSkied = jedis.smembers("skier:" + skierId + ":seasons:" + seasonId + ":days");
      int daysSkiedCount = daysSkied.size();

      long totalVertical = 0;
      JsonArray daysVerticals = new JsonArray();
      for (String day : daysSkied) {
        String verticalKey = "skier:" + skierId + ":seasons:" + seasonId + ":days:" + day + ":vertical";
        String verticalStr = jedis.get(verticalKey);

        long vertical = verticalStr != null ? Long.parseLong(verticalStr) : 0;
        totalVertical += vertical;

        JsonObject dayVertical = new JsonObject();
        dayVertical.addProperty("day", day);
        dayVertical.addProperty("vertical", vertical);
        daysVerticals.add(dayVertical);
      }

      JsonArray daysLifts = new JsonArray();
      Gson gson = new Gson();
      for (String day : daysSkied) {
        String liftsKey = "skier:" + skierId + ":seasons:" + seasonId + ":days:" + day + ":lifts";
        // Use lrange to retrieve the list of lifts
        List<String> lifts = jedis.lrange(liftsKey, 0, -1);

        JsonObject dayLifts = new JsonObject();
        dayLifts.addProperty("day", day);
        dayLifts.add("lifts", gson.toJsonTree(lifts));
        daysLifts.add(dayLifts);
      }

      String resortSkiersKey = "resort:" + resortId + ":days:" + dayId + ":skiers";
      long uniqueSkiers = jedis.scard(resortSkiersKey);

      JsonObject responseJson = new JsonObject();
      responseJson.addProperty("skierId", skierId);
      responseJson.addProperty("daysSkied", daysSkiedCount);
      responseJson.addProperty("totalVertical", totalVertical);
      responseJson.add("daysVerticals", daysVerticals);
      responseJson.add("daysLifts", daysLifts);
      responseJson.addProperty("uniqueSkiers", uniqueSkiers);

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(responseJson.toString());
    } catch (Exception e) {
      e.printStackTrace(); // Log the full stack trace to understand the exact failure
      try {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"error\":\"Error fetching data from Redis: " + e.getMessage() + "\"}");
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }
}

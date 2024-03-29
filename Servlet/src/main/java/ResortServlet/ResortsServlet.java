package ResortServlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
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
    response.setContentType("text/html"); // Set content type to HTML
    PrintWriter out;

    try {
      out = response.getWriter();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    String skierId = request.getParameter("skierId");
    String seasonId = request.getParameter("seasonId");
    String dayId = request.getParameter("dayId");
    String resortId = request.getParameter("resortId");

    // Early return on parameter check
    if (skierId == null || seasonId == null || dayId == null || resortId == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.write("<p>Missing required parameters.</p>");
      return;
    }

    try (Jedis jedis = new Jedis("34.228.116.2", 6379)) {
      jedis.auth("password");

      Set<String> daysSkied = jedis.smembers("skier:" + skierId + ":seasons:" + seasonId + ":days");
      int daysSkiedCount = daysSkied.size();

      long totalVertical = 0;
      JsonArray daysVerticals = new JsonArray();
      for (String day : daysSkied) {
        String verticalKey =
            "skier:" + skierId + ":seasons:" + seasonId + ":days:" + day + ":vertical";
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

      StringBuilder htmlResponse = new StringBuilder();
      htmlResponse.append("<html><head><title>Skier Information</title>")
          .append("<style>")
          .append("body { font-family: Arial, sans-serif; margin-top: 20px; text-align: center; }") // Reduced margin-top and added text-align
          .append("table { width: 60%; margin-left: auto; margin-right: auto; border-collapse: collapse; }") // Ensure table is centered
          .append("th, td { border: 1px solid #ddd; padding: 8px; }")
          .append("th { background-color: #f2f2f2; }")
          .append("</style></head><body>")
          .append("<h1>Skier Report for Resort ").append(resortId).append("</h1>") // The title will inherit body's text-align: center
          .append("<table>") // The table is centered using margin-left and margin-right set to auto
          .append("<tr><th>Attribute</th><th>Value</th></tr>")
          .append("<tr><td>Skier Id</td><td>").append(skierId).append("</td></tr>")
          .append("<tr><td>Days Skied</td><td>").append(daysSkiedCount).append("</td></tr>")
          .append("<tr><td>Total Vertical</td><td>").append(totalVertical).append("</td></tr>")
          .append("<tr><td>Unique Skiers</td><td>").append(uniqueSkiers).append("</td></tr>")
          .append("</table>")
          .append("</body></html>");

      htmlResponse.append("<h2>Days Verticals</h2>");
      daysVerticals.forEach(dayVertical -> {
        htmlResponse.append("<p>Day: ").append(dayVertical.getAsJsonObject().get("day").getAsString())
            .append(", Vertical: ").append(dayVertical.getAsJsonObject().get("vertical").getAsString())
            .append("</p>");
      });

      htmlResponse.append("<h2>Days Lifts</h2>");
      daysLifts.forEach(dayLift -> {
        htmlResponse.append("<p>Day: ").append(dayLift.getAsJsonObject().get("day").getAsString())
            .append(", Lifts: ");
        dayLift.getAsJsonObject().get("lifts").getAsJsonArray().forEach(lift ->
            htmlResponse.append(lift.getAsString()).append(" ")
        );
        htmlResponse.append("</p>");
      });

      htmlResponse.append("</body></html>");
      response.setStatus(HttpServletResponse.SC_OK);
      out.write(htmlResponse.toString());
    } catch (Exception e) {
      e.printStackTrace(); // Log the full stack trace
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      out.write("<p>Error fetching data from Redis: " + e.getMessage() + "</p>");
    }
  }
}

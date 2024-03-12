package ResortServlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ResortsServlet")
public class ResortsServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write("{\n" +
        "  \"top1Resort\": [\n" +
        "    {\n" +
        "      \"ResortID\": 44,\n" +
        "      \"Vertical\": 30400\n" +
        "    },\n" +
        "  { \"skierID\": 23433, \"Runs\": 22 } " +
        "  ]\n" +
        "}");
  }
}

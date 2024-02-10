package org.maven.proj;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.model.SeasonsList;

public class ApiClientTestGet {

  public static void main(String[] args) {

    ApiClient apiClient = new ApiClient();
//    apiClient.setBasePath("http://localhost:8080/Proj_6650_war_exploded");
    apiClient.setBasePath("http://54.82.39.47:8080/Proj-6650_war");
    ResortsApi apiInstance = new ResortsApi(apiClient);

    Integer resortID = 56; // Integer | ID of the resort of interest
    try {
      SeasonsList result = apiInstance.getResortSeasons(resortID);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResortsApi#getResortSeasons");
      e.printStackTrace();
      System.err.println("Status code: " + e.getCode());
      System.err.println("Response headers: " + e.getResponseHeaders());
      System.err.println("Response body: " + e.getResponseBody());
    }
  }

}

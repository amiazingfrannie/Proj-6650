package org.maven.proj;

import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;

public class ApiClientGetResortsTest {

  public static void main(String[] args) {

    ApiClient apiClient = new ApiClient();
//    apiClient.setBasePath("http://localhost:8080/Proj_6650_war_exploded");
    apiClient.setBasePath("http://54.82.39.47:8080/Proj-6650_war");
    ResortsApi apiInstance = new ResortsApi(apiClient);

    try {
      ResortsList result = apiInstance.getResorts();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResortsApi#getResorts");
      e.printStackTrace();
    }
  }

}

package org.maven.proj;

import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;

public class ApiClientTestAdd {

  public static void main(String[] args) {

    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost:8080/Proj_6650_war_exploded");
//    apiClient.setBasePath("http://54.82.39.47:8080/Proj-6650_war");
    ResortsApi apiInstance = new ResortsApi(apiClient);

    ResortIDSeasonsBody body = new ResortIDSeasonsBody(); // ResortIDSeasonsBody | Specify new Season value
    body.setYear("2024");
    Integer resortID = 56; // Integer | ID of the resort of interest
    try {
      apiInstance.addSeason(body, resortID);
      System.out.println(body);
      System.out.println("Season added successfully. " + apiInstance.getResorts());

      // Retrieve the list of seasons
      SeasonsList seasonsList = apiInstance.getResortSeasons(resortID);
      if (seasonsList != null) {
        System.out.println("Retrieved seasons: " + seasonsList);
      } else {
        System.out.println("No seasons retrieved.");
      }

    } catch (ApiException e) {
      System.err.println("Exception when calling ResortsApi#addSeason");
      e.printStackTrace();
    }

  }

}

package com.tools.fsserver.rest.v1;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class FSServerStatsResourceTest {

  /**
   * FSServerStatsResource REST API test for verifying the correct reading (from the properties
   * file) and returning of the fileUploadSizeLimit
   */
  @Test
  public void testGettingFileUploadSizeLimit() {
    given()
        .when()
        .get("/v1/stats/fileUploadSizeLimit")
        .then()
        .statusCode(200)
        .body(containsString("10M"));
  }
}

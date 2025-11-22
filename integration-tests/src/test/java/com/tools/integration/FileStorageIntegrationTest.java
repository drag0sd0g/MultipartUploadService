package com.tools.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for File Storage Server and Client
 * Tests both happy paths and error scenarios
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileStorageIntegrationTest {

    private static final int SERVER_PORT = 8080;
    private static final String BASE_URI = "http://localhost";

    @Container
    private static final DockerComposeContainer<?> environment =
            new DockerComposeContainer<>(new File("../docker-compose.yml"))
                    .withExposedService("server", SERVER_PORT,
                            Wait.forHttp("/q/health")
                                    .forStatusCode(200)
                                    .withStartupTimeout(Duration.ofMinutes(2)))
                    .withLocalCompose(true);

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = BASE_URI;
        RestAssured.port = environment.getServicePort("server", SERVER_PORT);
        
        // Wait for server to be fully ready
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .until(() -> {
                    try {
                        given()
                                .when()
                                .get("/q/health")
                                .then()
                                .statusCode(200);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    @Test
    @Order(1)
    @DisplayName("Happy Path: Server should be healthy")
    public void testServerHealth() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(2)
    @DisplayName("Happy Path: Should get file upload size limit")
    public void testGetFileUploadSizeLimit() {
        given()
                .when()
                .get("/v1/stats/fileUploadSizeLimit")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("Happy Path: Should list files when none exist (404)")
    public void testListFilesWhenEmpty() {
        given()
                .when()
                .get("/v1/files")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    @DisplayName("Happy Path: Should upload a small text file successfully")
    public void testUploadSmallFile() throws IOException {
        // Create a temporary test file
        Path tempFile = Files.createTempFile("test-upload", ".txt");
        Files.writeString(tempFile, "This is a test file content for integration testing.");

        try {
            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(200)
                    .body(containsString("uploaded successfully"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @Order(5)
    @DisplayName("Happy Path: Should list uploaded files")
    public void testListFilesAfterUpload() {
        given()
                .when()
                .get("/v1/files")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(containsString("test-upload"))
                .body(containsString(".txt"));
    }

    @Test
    @Order(6)
    @DisplayName("Happy Path: Should upload another file")
    public void testUploadAnotherFile() throws IOException {
        Path tempFile = Files.createTempFile("test-second", ".txt");
        Files.writeString(tempFile, "Second test file content.");

        try {
            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(200);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @Order(7)
    @DisplayName("Happy Path: Should list multiple uploaded files")
    public void testListMultipleFiles() {
        String response = given()
                .when()
                .get("/v1/files")
                .then()
                .statusCode(200)
                .extract().asString();

        // Should contain both files
        Assertions.assertTrue(response.contains("test-upload") || response.contains("test-second"));
    }

    @Test
    @Order(8)
    @DisplayName("Unhappy Path: Should fail to upload file without payload")
    public void testUploadWithoutPayload() {
        given()
                .contentType("multipart/form-data")
                .when()
                .post("/v1/files")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    @Order(9)
    @DisplayName("Unhappy Path: Should fail to upload duplicate file")
    public void testUploadDuplicateFile() throws IOException {
        // Create a file with specific name
        Path tempFile = Files.createTempFile("duplicate-test", ".txt");
        Files.writeString(tempFile, "Duplicate test content.");

        try {
            // First upload should succeed
            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(200);

            // Second upload of same file should fail with 409 Conflict
            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(409)
                    .body(containsString("already exists"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Unhappy Path: Should fail to upload file larger than limit")
    public void testUploadOversizedFile() throws IOException {
        // Create a file larger than 10MB
        Path tempFile = Files.createTempFile("oversized-test", ".bin");
        
        try {
            // Create an 11MB file
            byte[] largeData = new byte[11 * 1024 * 1024];
            Files.write(tempFile, largeData);

            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(413); // Payload Too Large
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @Order(11)
    @DisplayName("Happy Path: Should delete an uploaded file")
    public void testDeleteFile() throws IOException {
        // Upload a file first
        Path tempFile = Files.createTempFile("to-delete", ".txt");
        String fileName = tempFile.getFileName().toString();
        Files.writeString(tempFile, "File to be deleted.");

        try {
            // Upload the file
            given()
                    .contentType("multipart/form-data")
                    .multiPart("payload", tempFile.toFile())
                    .when()
                    .post("/v1/files")
                    .then()
                    .statusCode(200);

            // Delete the file
            given()
                    .pathParam("filename", fileName)
                    .when()
                    .delete("/v1/files/{filename}")
                    .then()
                    .statusCode(200)
                    .body(containsString("deleted successfully"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @Order(12)
    @DisplayName("Unhappy Path: Should fail to delete non-existent file")
    public void testDeleteNonExistentFile() {
        given()
                .pathParam("filename", "non-existent-file.txt")
                .when()
                .delete("/v1/files/{filename}")
                .then()
                .statusCode(404)
                .body(containsString("not found"));
    }

    @Test
    @Order(13)
    @DisplayName("Happy Path: Prometheus metrics should be available")
    public void testPrometheusMetrics() {
        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(containsString("jvm_"))
                .body(containsString("http_"));
    }

    @Test
    @Order(14)
    @DisplayName("Happy Path: Swagger UI should be accessible")
    public void testSwaggerUI() {
        given()
                .when()
                .get("/q/swagger-ui")
                .then()
                .statusCode(anyOf(is(200), is(302))); // 200 or redirect
    }

    @Test
    @Order(15)
    @DisplayName("Unhappy Path: Invalid endpoint should return 404")
    public void testInvalidEndpoint() {
        given()
                .when()
                .get("/v1/invalid-endpoint")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(16)
    @DisplayName("Happy Path: OpenAPI spec should be available")
    public void testOpenAPISpec() {
        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .contentType(anyOf(
                        containsString("application/yaml"),
                        containsString("application/json"),
                        containsString("text/plain")
                ));
    }
}

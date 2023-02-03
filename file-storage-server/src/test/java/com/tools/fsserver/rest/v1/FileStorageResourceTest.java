package com.tools.fsserver.rest.v1;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;
import com.tools.fsserver.storage.FileSystemStorageService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class FileStorageResourceTest {

  @Inject FileSystemStorageService fileSystemStorageService;

  @Test
  public void testListingFilesWhenUploadedFilesExist() throws IOException {
    Set<String> uploadedFiles = Set.of("a.txt", "b.gif", "c.png");
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    when(mock.listUploadedFiles()).thenReturn(uploadedFiles);
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .when()
        .get("/v1/files")
        .then()
        .statusCode(200)
        .body(containsString("a.txt"), containsString("b.gif"), containsString("c.png"));
  }

  @Test
  public void testListingFilesWhenNoUploadedFiles() throws IOException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    when(mock.listUploadedFiles()).thenReturn(new HashSet<>());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given().when().get("/v1/files").then().statusCode(404);
  }

  @Test
  public void testListingFilesWithIOException() throws IOException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    when(mock.listUploadedFiles()).thenThrow(new IOException());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .when()
        .get("/v1/files")
        .then()
        .statusCode(500)
        .body(containsString("An error occurred when listing uploaded files."));
  }

  @Test
  public void testUploadingWithNoMultipartPayloadExpect400() {
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .when()
        .post("/v1/files/f1.txt")
        .then()
        .statusCode(400);
  }

  @Test
  public void testUploadingFileSuccessfully() {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "123")
        .when()
        .post("/v1/files/f2.txt")
        .then()
        .statusCode(200)
        .body(containsString("File uploaded successfully"));
  }

  @Test
  public void testUploadingSameFileExpectConflict()
      throws IOException, FileNamePresentOnServerException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    doThrow(new FileNamePresentOnServerException("file exists"))
        .when(mock)
        .storeFile(any(), any());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "123")
        .when()
        .post("/v1/files/f3.txt")
        .then()
        .statusCode(409)
        .body(containsString("already exists on server"));
  }

  @Test
  public void testUploadingFileWithIOException()
      throws IOException, FileNamePresentOnServerException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    doThrow(new IOException()).when(mock).storeFile(any(), any());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "555")
        .when()
        .post("/v1/files/f33.txt")
        .then()
        .statusCode(500)
        .body(containsString("An error occurred during file upload."));
  }

  @Test
  public void testDeletingFileSuccessfully() {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "123")
        .when()
        .delete("/v1/files/f102.txt")
        .then()
        .statusCode(200)
        .body(containsString("File deleted successfully"));
  }

  @Test
  public void testDeletingInexistentFile() throws FileNameNotPresentOnServerException, IOException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    doThrow(new FileNameNotPresentOnServerException("non existent")).when(mock).deleteFile(any());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "123")
        .when()
        .delete("/v1/files/f55.txt")
        .then()
        .statusCode(404)
        .body(containsString("does not exist on server"));
  }

  @Test
  public void testDeletingFileWithIOException()
      throws IOException, FileNameNotPresentOnServerException {
    FileSystemStorageService mock = mock(FileSystemStorageService.class);
    doThrow(new IOException()).when(mock).deleteFile(any());
    QuarkusMock.installMockForInstance(mock, fileSystemStorageService);
    given()
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .multiPart("payload", "abcdef")
        .when()
        .delete("/v1/files/f12345.txt")
        .then()
        .statusCode(500)
        .body(containsString("An error occurred during file deletion."));
  }
}

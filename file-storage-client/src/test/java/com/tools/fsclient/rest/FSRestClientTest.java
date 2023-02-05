package com.tools.fsclient.rest;

import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 * Tests for the CLI's HTTP interaction with the server for each of the upload/delete/list
 * scenarios. The HTTP interaction is mocked and the purpose of these tests is to verify that the
 * HTTP statuses received from the server are translated to user-friendly messages
 */
public class FSRestClientTest {

  private static final String MOCKED_LOGGER = "LOG";
  private static final String TEST_FILES_API = "http://localhost:8080/v1/files";
  private static final String TEST_STATS_API = "http://localhost:8080/v1/stats";
  private static final String TEST_FILE_TO_DELETE = "f1.txt";
  private static final String TEST_FILE_TO_UPLOAD = "f2.txt";

  @Test
  public void testSuccessfulUploadFileCall() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger).info(any(String.class), eq(TEST_FILE_TO_UPLOAD));
  }

  @Test
  public void testUploadFileCallWithIOException() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doThrow(IOException.class).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger).error(eq("Error uploading file. Please try again"));
  }

  @Test
  public void testUploadATooLargeFile() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    doReturn("7G").when(fsRestClient).getFileUploadSizeLimit();
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_REQUEST_TOO_LONG);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger)
        .error(
            eq("{} is larger than size limit of {}. Please try again with smaller files"),
            eq(TEST_FILE_TO_UPLOAD),
            eq("7G"));
  }

  @Test
  public void testUploadFileCallWithBadRequest() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger).error(eq("Upload error. Missing 'payload' from multipart body"));
  }

  @Test
  public void testUploadFileCallWithConflictDueToDuplication() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_CONFLICT);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger)
        .error(eq("Upload error. {} already exists on server"), eq(TEST_FILE_TO_UPLOAD));
  }

  @Test
  public void testUploadFileCallWithInternalServerError() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger)
        .error(
            eq("Unexpected server error when uploading file {}. Please try again"),
            eq(TEST_FILE_TO_UPLOAD));
  }

  @Test
  public void testUploadFileCallWithUnexpectedErrorCode() throws IOException {
    Logger mockLogger = mock(Logger.class);
    Path pathToUploadedFile = mock(Path.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doReturn(pathToUploadedFile).when(fsRestClient).resolvePathToUploadFile(TEST_FILE_TO_UPLOAD);
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_FAILED_DEPENDENCY);
    doReturn(mockResponse).when(fsRestClient).serverCallToUploadFile(pathToUploadedFile);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.uploadFile(TEST_FILE_TO_UPLOAD);
    verify(mockLogger)
        .error(
            eq("Unexpected error when uploading file {}. Please try again"),
            eq(TEST_FILE_TO_UPLOAD));
  }

  @Test
  public void testSuccessfulListFilesCall() throws IOException, ParseException {
    String expectedUploadedFilesCsv = "f1.txt,f2.txt,f3.txt";
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);
    doReturn(mockResponse).when(fsRestClient).serverCallToListUploadedFiles();
    doReturn(expectedUploadedFilesCsv).when(fsRestClient).convertHttpEntityToString(any());
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.listUploadedFiles();
    verify(mockLogger).info(any(String.class), eq(expectedUploadedFilesCsv));
  }

  @Test
  public void testListFilesCallWithNoFilesReturned() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    doReturn(mockResponse).when(fsRestClient).serverCallToListUploadedFiles();
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.listUploadedFiles();
    verify(mockLogger).warn(eq("No files have been uploaded yet"));
  }

  @Test
  public void testListFilesCallWithInternalServerErrorCode() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    doReturn(mockResponse).when(fsRestClient).serverCallToListUploadedFiles();
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.listUploadedFiles();
    verify(mockLogger)
        .error(eq("Unexpected server error when listing uploaded files. Please try again"));
  }

  @Test
  public void testListFilesCallWithUnexpectedErrorCode() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);
    doReturn(mockResponse).when(fsRestClient).serverCallToListUploadedFiles();
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.listUploadedFiles();
    verify(mockLogger).error(eq("Unexpected error when listing uploaded files. Please try again"));
  }

  @Test
  public void testListFilesCallWithIOException() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doThrow(IOException.class).when(fsRestClient).serverCallToListUploadedFiles();
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.listUploadedFiles();
    verify(mockLogger).error(eq("Error fetching list of all uploaded files. Please try again"));
  }

  @Test
  public void testSuccessfulDeleteFileCall() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);
    doReturn(mockResponse).when(fsRestClient).serverCallToDeleteFile(TEST_FILE_TO_DELETE);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.deleteFile(TEST_FILE_TO_DELETE);
    verify(mockLogger).info(any(String.class), eq(TEST_FILE_TO_DELETE));
  }

  @Test
  public void testDeleteFileCallButNoFileFound() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    doReturn(mockResponse).when(fsRestClient).serverCallToDeleteFile(TEST_FILE_TO_DELETE);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.deleteFile(TEST_FILE_TO_DELETE);
    verify(mockLogger)
        .error(
            eq("Did not delete anything. File {} is not present on server"),
            eq(TEST_FILE_TO_DELETE));
  }

  @Test
  public void testDeleteFileCallWithInternalServerError() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    doReturn(mockResponse).when(fsRestClient).serverCallToDeleteFile(TEST_FILE_TO_DELETE);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.deleteFile(TEST_FILE_TO_DELETE);
    verify(mockLogger)
        .error(
            eq("Unexpected server error when deleting file {}. Please try again"),
            eq(TEST_FILE_TO_DELETE));
  }

  @Test
  public void testDeleteFileCallWithUnexpectedError() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    BasicClassicHttpResponse mockResponse = mock(BasicClassicHttpResponse.class);
    when(mockResponse.getCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
    doReturn(mockResponse).when(fsRestClient).serverCallToDeleteFile(TEST_FILE_TO_DELETE);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.deleteFile(TEST_FILE_TO_DELETE);
    verify(mockLogger)
        .error(
            eq("Unexpected error when deleting file {}. Please try again"),
            eq(TEST_FILE_TO_DELETE));
  }

  @Test
  public void testDeleteFileCallWithIOException() throws IOException {
    Logger mockLogger = mock(Logger.class);
    FSRestClient fsRestClient = spy(new FSRestClient(TEST_FILES_API, TEST_STATS_API));
    doThrow(IOException.class).when(fsRestClient).serverCallToDeleteFile(TEST_FILE_TO_DELETE);
    Whitebox.setInternalState(FSRestClient.class, MOCKED_LOGGER, mockLogger);
    fsRestClient.deleteFile(TEST_FILE_TO_DELETE);
    verify(mockLogger).error(eq("Error deleting file. Please try again"));
  }
}

package com.tools.fsclient.rest;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FSRestClient {

  private static final Logger LOG = LoggerFactory.getLogger(FSRestClient.class);
  private static final String MULTIPART_UPLOAD_PAYLOAD_NAME = "payload";
  private static final String FILE_UPLOAD_SIZE_LIMIT_ENDPOINT = "fileUploadSizeLimit";
  private final String serverFilesApi;
  private final String serverStatsApi;
  private String cachedFileUploadSizeLimit = "";

  public FSRestClient(String serverFilesApi, String serverStatsApi) {
    this.serverFilesApi = serverFilesApi;
    this.serverStatsApi = serverStatsApi;
  }

  /**
   * Makes an HTTP GET request to the storage server to fetch a list of all uploaded files.
   * Based on the response's HTTP status code, it translates the outcome into a user-friendly log message.
   * Expected server codes:
   * 200 OK - list of uploaded files returned successfully
   * 404 Not found - no uploaded files found on server
   * 500 Internal Server Error - something went wrong server-side when fetching uploaded files
   */
  public void listUploadedFiles() {
    LOG.debug("Requesting list of all uploaded files");
    BasicClassicHttpResponse httpResponse = null;
    HttpEntity entity = null;
    try {
      httpResponse = serverCallToListUploadedFiles();
      entity = httpResponse.getEntity();
      switch (httpResponse.getCode()) {
        case HttpStatus.SC_OK -> LOG.info("Currently uploaded files: {}", convertHttpEntityToString(entity));
        case HttpStatus.SC_NOT_FOUND -> LOG.warn("No files have been uploaded yet");
        case HttpStatus.SC_INTERNAL_SERVER_ERROR -> LOG.error("Unexpected server error when listing uploaded files. Please try again");
        default -> LOG.error("Unexpected error when listing uploaded files. Please try again");
      }
    } catch (IOException | ParseException e) {
      LOG.error("Error fetching list of all uploaded files. Please try again");
    } finally {
      try {
        cleanUpHttpResourcesIfNecessary(entity, httpResponse);
      } catch (IOException e) {
        LOG.error("Error fetching list of all uploaded files. Please try again");
      }
    }
  }

  /**
   * Makes an HTTP POST request to the storage server to upload a local file.
   * Based on the response's HTTP status code, it translates the outcome into a user-friendly log message.
   * Expected server codes:
   * 200 OK - file uploaded successfully
   * 400 Bad Request - Mandatory 'payload' multipart body missing from the request
   * 413 Request Entity Too Large - file size exceeds the server-set boundary
   * 409 Conflict - duplicate upload detected
   * 500 Internal Server Error - something went wrong server-side during upload
   *
   * @param fileNameToUpload - The file we want to upload - this must exist locally and be within the
   *                         size limit mandated by the server
   */
  public void uploadFile(String fileNameToUpload) {
    LOG.debug("Requesting to upload the file {}", fileNameToUpload);
    Path fileToUpload = resolvePathToUploadFile(fileNameToUpload);
    BasicClassicHttpResponse httpResponse = null;
    try {
      httpResponse = serverCallToUploadFile(fileToUpload);
      switch (httpResponse.getCode()) {
        case HttpStatus.SC_OK -> LOG.info("Successfully uploaded file {}", fileNameToUpload);
        case HttpStatus.SC_BAD_REQUEST -> LOG.error("Upload error. Missing 'payload' from multipart body");
        case HttpStatus.SC_CONFLICT -> LOG.error("Upload error. {} already exists on server", fileNameToUpload);
        case HttpStatus.SC_REQUEST_TOO_LONG -> LOG.error("{} is larger than size limit of {}. Please try again with smaller files", fileNameToUpload, getFileUploadSizeLimit());
        case HttpStatus.SC_INTERNAL_SERVER_ERROR -> LOG.error("Unexpected server error when uploading file {}. Please try again", fileNameToUpload);
        default -> LOG.error("Unexpected error when uploading file {}. Please try again", fileNameToUpload);
      }
    } catch (IOException e) {
      LOG.error("Error uploading file. Please try again");
    } finally {
      try {
        cleanUpHttpResourcesIfNecessary(null, httpResponse);
      } catch (IOException e) {
        LOG.error("Error uploading file. Please try again");
      }
    }
  }

  /**
   * Makes an HTTP DELETE request to the storage server to delete a previously-uploaded file
   * Based on the response's HTTP status code, it translates the outcome into a user-friendly log message.
   * Expected server codes:
   * 200 OK - file deleted successfully
   * 404 Not found - provided file for deletion was not found on server
   * 500 Internal Server Error - something went wrong server-side during deletion
   *
   * @param fileNameToDelete - The name of the previously-uploaded file we want to delete. (This file doesn't need to be
   *                         physically present locally)
   */
  public void deleteFile(String fileNameToDelete) {
    LOG.debug("Requesting for deletion {}", fileNameToDelete);
    BasicClassicHttpResponse httpResponse = null;
    try {
      httpResponse = serverCallToDeleteFile(fileNameToDelete);
      switch (httpResponse.getCode()) {
        case HttpStatus.SC_OK -> LOG.info("Successfully deleted file {}", fileNameToDelete);
        case HttpStatus.SC_NOT_FOUND -> LOG.error("Did not delete anything. File {} is not present on server", fileNameToDelete);
        case HttpStatus.SC_INTERNAL_SERVER_ERROR -> LOG.error("Unexpected server error when deleting file {}. Please try again", fileNameToDelete);
        default -> LOG.error("Unexpected error when deleting file {}. Please try again", fileNameToDelete);
      }
    } catch (IOException e) {
      LOG.error("Error deleting file. Please try again");
    } finally {
      try {
        cleanUpHttpResourcesIfNecessary(null, httpResponse);
      } catch (IOException e) {
        LOG.error("Error deleting file. Please try again");
      }
    }
  }

  /**
   * Makes an HTTP GET to the server's /stats/fileUploadSizeLimit API to fetch and cache the upload size limit
   * for a file
   * @return The file size limit that can be uploaded to the server
   */
  public String getFileUploadSizeLimit(){
    if (StringUtils.isEmpty(this.cachedFileUploadSizeLimit)) { //only fetch if not cached already
      try {
        this.cachedFileUploadSizeLimit = Request.get(this.serverStatsApi + "/" + FILE_UPLOAD_SIZE_LIMIT_ENDPOINT)
                .execute()
                .returnContent()
                .asString();
      } catch (IOException e) {
        LOG.debug("Error contacting the server to get the file upload size limit. " +
                "This is fine for now as the server will still restrict uploading of files above the configured limit");
      }
    }
    return this.cachedFileUploadSizeLimit;
  }

  @VisibleForTesting
  BasicClassicHttpResponse serverCallToUploadFile(Path fileToUpload) throws IOException {
    HttpEntity multiPartEntity = MultipartEntityBuilder.create()
            .addBinaryBody(MULTIPART_UPLOAD_PAYLOAD_NAME, fileToUpload.toAbsolutePath().toFile())
            .build();
    //This encoding ensures we deal with file names which may contain spaces
    String encodedFileName = URLEncoder.encode(fileToUpload.getFileName().toString(), Charset.defaultCharset());
    return  (BasicClassicHttpResponse) Request.post(this.serverFilesApi + "/" + encodedFileName)
                            .body(multiPartEntity)
                            .useExpectContinue()
                            .execute()
                            .returnResponse();
  }

  @VisibleForTesting
  BasicClassicHttpResponse serverCallToDeleteFile(String file) throws IOException {
    return (BasicClassicHttpResponse)
            Request.delete(this.serverFilesApi + "/" + file).execute().returnResponse();
  }

  @VisibleForTesting
  BasicClassicHttpResponse serverCallToListUploadedFiles() throws IOException {
    return (BasicClassicHttpResponse) Request.get(this.serverFilesApi).execute().returnResponse();
  }

  @VisibleForTesting
  String convertHttpEntityToString(HttpEntity entity) throws IOException, ParseException {
    return EntityUtils.toString(entity);
  }

  @VisibleForTesting
  Path resolvePathToUploadFile(String fileNameToUpload){
    return Paths.get(fileNameToUpload);
  }

  public String getServerFilesApi() {
    return serverFilesApi;
  }

  public String getServerStatsApi() {
    return serverStatsApi;
  }

  private void cleanUpHttpResourcesIfNecessary(
          HttpEntity httpEntity, BasicClassicHttpResponse httpResponse) throws IOException {
    if (!Objects.isNull(httpEntity)) {
      EntityUtils.consume(httpEntity);
    }
    if (!Objects.isNull(httpResponse)) {
      httpResponse.close();
    }
  }
}

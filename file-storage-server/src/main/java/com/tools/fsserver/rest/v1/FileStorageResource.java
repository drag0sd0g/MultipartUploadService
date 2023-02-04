package com.tools.fsserver.rest.v1;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;
import com.tools.fsserver.storage.IStorageService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * V1 of the /files REST API. Encapsulates a generic IStorageService to which it delegates storage
 * of data in case of uploads, listing of stored files, deletions. Each operation of the REST API is
 * annotated with all possible HTTP status code responses with respect to the provided input. In
 * order to visualize the REST API docs, start up the server (as instructed in the README.md) and
 * navigate to http://<server_host>:<server_port>/q/swagger-ui
 */
@Tag(
    name = "File Storage Server main REST API",
    description = "provides operations for uploading, deleting and listing uploaded files")
@Path("/v1/files")
public class FileStorageResource {

  private static final Logger LOG = Logger.getLogger(FileStorageResource.class);
  private static final String COMMON_SERVER_ERROR_MESSAGE_SUFFIX = " Please try again";

  private final IStorageService storageService;

  @Inject
  public FileStorageResource(IStorageService storageService) {
    this.storageService = storageService;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Returns a list of all uploaded file names")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "Uploaded files found"),
    @APIResponse(responseCode = "404", description = "No uploaded files found"),
    @APIResponse(responseCode = "500", description = "Unexpected server error")
  })
  public Response listFiles() {
    LOG.debug("Received request to list all uploaded files");
    try {
      String uploadedFileNamesCsv =
          convertUploadedFileNamesToCsv(this.storageService.listStoredFiles());
      return uploadedFileNamesCsv.isEmpty()
          ? Response.status(Response.Status.NOT_FOUND).build()
          : Response.status(Response.Status.OK)
              .entity(this.storageService.listStoredFiles())
              .build();
    } catch (IOException e) {
      String errMsg = "An error occurred when listing uploaded files.";
      LOG.error(errMsg, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(errMsg + COMMON_SERVER_ERROR_MESSAGE_SUFFIX)
          .build();
    }
  }

  private String convertUploadedFileNamesToCsv(Set<String> uploadedFileNames) {
    return String.join(",", uploadedFileNames);
  }

  @POST
  @Path("{fileName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Uploads a file to data-server folder")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "File uploaded successfully"),
    @APIResponse(responseCode = "400", description = "Request without a multipart 'payload' body"),
    @APIResponse(responseCode = "409", description = "Attempting to upload a duplicate"),
    @APIResponse(
        responseCode = "413",
        description = "Attempting to upload a file larger than the size limit"),
    @APIResponse(responseCode = "500", description = "Unexpected server error")
  })
  public Response uploadFile(
      @PathParam("fileName") String persistentFileName,
      @RestForm("payload") java.nio.file.Path pathToTempUploadLocation) {
    LOG.debug("Received request to upload file " + persistentFileName);
    if (Objects.isNull(pathToTempUploadLocation)) {
      String errMsg = "Request did not contain a multipart 'payload' body";
      LOG.error(errMsg);
      return Response.status(Response.Status.BAD_REQUEST).entity(errMsg).build();
    }
    try {
      this.storageService.storeFile(persistentFileName, pathToTempUploadLocation);
    } catch (FileNamePresentOnServerException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(persistentFileName + " already exists on server")
          .build();
    } catch (IOException e) {
      String errMsg = "An error occurred during file upload.";
      LOG.error(errMsg, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(errMsg + COMMON_SERVER_ERROR_MESSAGE_SUFFIX)
          .build();
    }
    return Response.status(Response.Status.OK).entity("File uploaded successfully").build();
  }

  @DELETE
  @Path("{fileName}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Deletes a file from data-server folder")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "File deleted successfully"),
    @APIResponse(responseCode = "404", description = "File not uploaded on server"),
    @APIResponse(responseCode = "500", description = "Unexpected server error")
  })
  public Response deleteFile(@PathParam("fileName") String fileName) {
    LOG.debug("Received request to delete file " + fileName);
    try {
      this.storageService.deleteFile(fileName);
    } catch (FileNameNotPresentOnServerException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(fileName + " does not exist on server")
          .build();
    } catch (IOException e) {
      String errMsg = "An error occurred during file deletion.";
      LOG.error(errMsg, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(errMsg + COMMON_SERVER_ERROR_MESSAGE_SUFFIX)
          .build();
    }
    return Response.status(Response.Status.OK).entity("File deleted successfully").build();
  }
}

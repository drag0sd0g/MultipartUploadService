package com.tools.fsserver.rest.v1;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * V1 of the /stats REST API. Exposes information about the server which potential clients may be
 * interested in. For example maximum allowed file size for uploads. These operations are also
 * visible in the Swagger UI at http://<server_host>:<server_port>/q/swagger-ui
 */
@Tag(
    name = "File Storage Server stats REST API",
    description = "provides server side information like file upload size limit")
@Path("/v1/stats")
public class FSServerStatsResource {

  private final String fileUploadSizeLimit;

  @Inject
  public FSServerStatsResource(
      @ConfigProperty(name = "quarkus.http.limits.max-form-attribute-size")
          String fileUploadSizeLimit) {
    this.fileUploadSizeLimit = fileUploadSizeLimit;
  }

  @GET
  @Path("/fileUploadSizeLimit")
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Returns the file upload size limit configured on this server")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "File upload size limit returned successfully")
  })
  public Response fileUploadSizeLimit() {
    return Response.status(Response.Status.OK).entity(this.fileUploadSizeLimit).build();
  }
}

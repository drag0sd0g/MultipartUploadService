package com.tools.fsserver.rest.v1;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

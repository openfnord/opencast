/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.editor.api;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link EditorService} service
 */
public abstract class EditorRestEndpointBase {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(EditorRestEndpointBase.class);

  /** The service */
  protected EditorService editorService;

  public abstract void setEditorService(EditorService service);

  @GET
  @Path("{mediaPackageId}/edit.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getEditorData",
          description = "Returns all the information required to get the editor tool started",
          returnDescription = "JSON object",
          pathParameters = { @RestParameter(name = "mediaPackageId", description = "The id of the media package",
                  isRequired = true, type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "Media package found", responseCode = SC_OK),
          @RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND) })
  public Response getEditorData(@PathParam("mediaPackageId") final String mediaPackageId) {
    try {
      EditingData response = editorService.getEditData(mediaPackageId);
      if (response != null) {
        return Response.ok(response.toString(), MediaType.APPLICATION_JSON_TYPE).build();
      } else {
        logger.error("EditorService returned empty response");
        return RestUtil.R.serverError();
      }
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).entity("No write access to this event.").build();
    }
  }

  @POST
  @Path("{mediaPackageId}/lock")
  @RestQuery(name = "lockMediapackage",
          description = "Creates and updates the lock for a mediapackage in the editor. "
          + "Requests will create/refreshen a lock at /editor/{mediapackageId}/lock{uuid} "
          + "(see Location header in response) that will expire after the configured period. "
          + "Subsequent calls must have the same uuid, which will then freshen the lock.",
          returnDescription = "The lock is returned in the Location header.",
          pathParameters = {
            @RestParameter(name = "mediaPackageId", description = "The id of the media package", isRequired = true,
                    type = RestParameter.Type.STRING)
          },
          restParameters = {
            @RestParameter(name = "user", isRequired = true,
                description = "The user requesting to lock this mediapackage",
                type = RestParameter.Type.STRING, defaultValue = "admin"),
            @RestParameter(name = "uuid", isRequired = true,
                description = "The unique identitier of the lock",
                type = RestParameter.Type.STRING)
          },
          responses = {
            @RestResponse(description = "Lock obtained", responseCode = SC_CREATED),
            @RestResponse(description = "Lock not obtained", responseCode = SC_CONFLICT),
            @RestResponse(description = "Mediapackage not found", responseCode = SC_NOT_FOUND)
          })
  public Response lockMediapackage(@PathParam("mediaPackageId") final String mediaPackageId,
         @FormParam("user") final String user, @FormParam("uuid") final String uuid,
         @Context HttpServletRequest request) {
    try {
      LockData lockData = new LockData(uuid, user);
      editorService.lockMediaPackage(mediaPackageId, lockData);
      return RestUtil.R.created(new URI(request.getRequestURI() + "/" + lockData.getUUID()));
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    } catch (Exception e) {
      logger.debug("Unable to create lock",  e);
      return RestUtil.R.badRequest();
    }
  }

  @DELETE
  @Path("{mediaPackageId}/lock/{uuid}")
  @RestQuery(name = "unlockMediapackage",
          description = "Releases the lock for a mediapackage in the editor",
          returnDescription = "",
          pathParameters = {
            @RestParameter(name = "mediaPackageId", description = "The id of the media package", isRequired = true,
                    type = RestParameter.Type.STRING),
            @RestParameter(name = "uuid", description = "Identifier of editor session", isRequired = true,
                    type = RestParameter.Type.STRING)
          },
          responses = {
            @RestResponse(description = "Lock deleted", responseCode = SC_OK),
            @RestResponse(description = "Lock not obtained", responseCode = SC_CONFLICT),
            @RestResponse(description = "Lock not found", responseCode = SC_NOT_FOUND)
          })

  public Response unlockMediapackage(@PathParam("mediaPackageId") final String mediaPackageId,
          @PathParam("uuid") final String uuid)  {
    try {
      editorService.unlockMediaPackage(mediaPackageId, new LockData(uuid, ""));
      return RestUtil.R.ok();
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    }
  }

  @POST
  @Path("{mediaPackageId}/edit.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it",
          returnDescription = "",
          pathParameters = {
          @RestParameter(name = "mediaPackageId", description = "The id of the media package", isRequired = true,
                  type = RestParameter.Type.STRING) },
          responses = {
          @RestResponse(description = "Editing information saved and processed", responseCode = SC_OK),
          @RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND),
          @RestResponse(description = "The editing information cannot be parsed",
                  responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response editVideo(@PathParam("mediaPackageId") final String mediaPackageId,
          @Context HttpServletRequest request) {
    String details = null;
    try {
      details = readInputStream(request);
      logger.debug("Editor POST-Request received: {}", details);
      EditingData editingInfo = EditingData.parse(details);
      editorService.setEditData(mediaPackageId, editingInfo);
    } catch (EditorServiceException e) {
      return checkErrorState(mediaPackageId, e);
    } catch (Exception e) {
      logger.debug("Unable to parse editing information ({})", details, e);
      return RestUtil.R.badRequest("Unable to parse details");
    }

    return RestUtil.R.ok();
  }

  protected String readInputStream(HttpServletRequest request) {
    String details;
    try (InputStream is = request.getInputStream()) {
      details = IOUtils.toString(is, request.getCharacterEncoding());
    } catch (IOException e) {
      logger.error("Error reading request body:", e);
      return null;
    }
    return details;
  }

  protected Response checkErrorState(final String eventId, EditorServiceException e) {
    switch (e.getErrorStatus()) {
      case MEDIAPACKAGE_NOT_FOUND:
        return RestUtil.R.notFound(String.format("Event '%s' not Found", eventId), MediaType.TEXT_PLAIN_TYPE);
      case MEDIAPACKAGE_LOCKED:
        return RestUtil.R.conflict(String.format("Event '%s' is %s", eventId, e.getMessage()));
      case WORKFLOW_ACTIVE:
        return RestUtil.R.locked();
      case WORKFLOW_NOT_FOUND:
      case NO_INTERNAL_PUBLICATION:
        return RestUtil.R.badRequest(e.getMessage());
      case UNABLE_TO_CREATE_CATALOG:
      case WORKFLOW_ERROR:
      case UNKNOWN:
      default:
        return RestUtil.R.serverError();
    }
  }

  @GET
  @Path("{mediaPackageId}/metadata.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getMetadata",
          description = "Returns all the data related to the metadata tab in the event details modal as JSON",
          returnDescription = "All the data related to the event metadata tab as JSON",
          pathParameters = {
          @RestParameter(name = "mediaPackageId", description = "The event id", isRequired = true,
                  type = RestParameter.Type.STRING) },
          responses = {
          @RestResponse(description = "Returns all the data related to the event metadata tab as JSON",
                  responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "No event with this identifier was found.",
                  responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getEventMetadata(@PathParam("mediaPackageId") String eventId) {
    try {
      String response = editorService.getMetadata(eventId);
      if (response != null) {
        return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
      } else {
        logger.error("EditorService returned empty response");
        return RestUtil.R.serverError();
      }
    } catch (EditorServiceException e) {
      return checkErrorState(eventId, e);
    }
  }
}

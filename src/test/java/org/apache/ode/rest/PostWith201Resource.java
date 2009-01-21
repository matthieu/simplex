package org.apache.ode.rest;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/post201")
public class PostWith201Resource {

    @POST
    @Consumes("application/xml")
    @Produces("application/xml")
    public Response post() {
        return Response.status(201).header("Location", "http://foo/bar").build();
    }
}

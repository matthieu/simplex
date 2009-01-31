package org.apache.ode.rest;

import org.apache.ode.utils.DOMUtils;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import javax.ws.rs.*;
import java.io.IOException;

@Path("/gppd")
public class TestAllMethodsResource {

    @GET
    @Produces("application/xml")
    public String get() {
        return "<get>GET</get>";
    }

    @POST
    @Consumes("application/xml")
    @Produces("application/xml")
    public String post(String input) {
        String out = "";
        try {
            Element root = DOMUtils.stringToDOM(input);
            out = root.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<post>POST" + out + "</post>";
    }

    @PUT
    @Consumes("application/xml")
    @Produces("application/xml")
    public String put(String input) {
        String out = "";
        try {
            Element root = DOMUtils.stringToDOM(input);
            out = root.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<put>PUT" + out + "</put>";
    }

    @DELETE
    public void delete() {
        System.out.println("*** DELETE ***");
    }

}

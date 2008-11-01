package org.apache.ode.rest;

import org.apache.ode.bpel.iapi.Resource;
import org.apache.ode.bpel.iapi.RESTMessageExchange;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.embed.ServerLifecycle;
import org.apache.ode.utils.GUID;
import org.apache.ode.utils.DOMUtils;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import java.io.IOException;

public class ProcessWebResource {

    private Resource _resource;
    private static ServerLifecycle _serverLifecyle;

    public ProcessWebResource(Resource resource, ServerLifecycle serverLifecyle) {
        _resource = resource;
        _serverLifecyle = serverLifecyle;
    }

    @GET @Produces("application/xml")
    public Response get() {
        if ("GET".equals(_resource.getMethod())) {

        }
        // TODO use the resource supported methods
        return Response.status(405).header("Allow", "POST").build();
    }

    @POST @Consumes("application/xml")
    public Response post(String content) {
        if ("POST".equals(_resource.getMethod())) {
            RESTMessageExchange mex = _serverLifecyle.getServer().createMessageExchange(_resource, new GUID().toString());
            Message request = mex.createMessage(null);
            try {
                // TODO support for http headers and parameters as additional parts
                Element msgElmt = DOMUtils.stringToDOM(content);
                Document doc = DOMUtils.newDocument();
                Element docElmt = doc.createElement("document");
                Element partElmt = doc.createElement("payload");
                doc.appendChild(docElmt);
                docElmt.appendChild(partElmt);
                partElmt.appendChild(doc.importNode(msgElmt, true));

                request.setMessage(docElmt);
            } catch (Exception e) {
                return Response.status(400).entity("Couldn't parse XML request.").type("text/plain").build();
            }
            mex.setRequest(request);
            try {
                mex.invokeBlocking();
            } catch (java.util.concurrent.TimeoutException te) {
                return Response.status(408).entity("The server timed out while processing the request.").build();
            }

            if (mex.getResponse() == null) {
                return Response.status(204).build();
            } else {
                return Response.status(200)
                        .entity(DOMUtils.domToString(DOMUtils.getFirstChildElement(DOMUtils
                                .getFirstChildElement(mex.getResponse().getMessage()))))
                        .type("application/xml")
                        .header("Location", mex.getResource().getUrl())
                        .build();
            }

        }
        // TODO use the resource supported methods
        return Response.status(405).header("Allow", "GET").build();
    }

//    @GET @Produces("application/xhtml+xml")
//    public String getXHTML() {
//        StringBuffer res = new StringBuffer();
//        res.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><body>");
//        res.append("<h3>Process ").append(_service).append("</h3>");
//        res.append("<p>To start the process POST an appropriate representation <a href=\"");
//        res.append("process/").append(_service.getLocalPart());
//        res.append("\">").append("process/").append(_service.getLocalPart()).append("</a>");
//        res.append("</p>");
//        res.append("</body></html>");
//        return res.toString();
//    }
//
//    @GET @Produces("application/xml")
//    public String getXML() {
//        StringBuffer res = new StringBuffer();
//        res.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//        res.append("<process>");
//        res.append("<start>").append("process/").append(_service.getLocalPart()).append("</start>");
//        res.append("</process>");
//        return res.toString();
//    }
}
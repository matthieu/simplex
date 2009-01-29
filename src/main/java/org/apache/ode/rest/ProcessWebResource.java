package org.apache.ode.rest;

import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.RESTInMessageExchange;
import org.apache.ode.embed.ServerLifecycle;
import org.apache.ode.utils.GUID;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.Map;

public class ProcessWebResource {

    private EngineWebResource.ResourceDesc _resource;
    private static ServerLifecycle _serverLifecyle;
    private String _root;

    public ProcessWebResource(EngineWebResource.ResourceDesc resource, ServerLifecycle serverLifecyle, String root) {
        _resource = resource;
        _serverLifecyle = serverLifecyle;
        _root = root;
    }

    @GET @Produces("application/xml")
    public Response get() {
        if (_resource.get) {
            RESTInMessageExchange mex = _serverLifecyle.getServer().createMessageExchange(
                    _resource.toResource("GET"), new GUID().toString());
            try {
                mex.invokeBlocking();
            } catch (java.util.concurrent.TimeoutException te) {
                return Response.status(408).entity("The server timed out while processing the request.").build();
            }

            if (mex.getResponse() == null) {
                return Response.status(204).build();
            } else {
                return Response.status(200)
                        .entity(unwrapResponse(mex.getResponse().getMessage()))
                        .type("application/xml")
                        .header("Location", _root+mex.getResource().getUrl())
                        .build();
            }
        }
        else return Response.status(405).header("Allow", _resource.methods()).build();
    }

    public Response post(Element msgElmt) {
        if (_resource.post) {
            RESTInMessageExchange mex = _serverLifecyle.getServer().createMessageExchange(
                    _resource.toResource("POST"), new GUID().toString());
            Message request = mex.createMessage(null);
            if (msgElmt != null) {
                // TODO support for http headers and parameters as additional parts
                Document doc = DOMUtils.newDocument();
                Element docElmt = doc.createElement("document");
                Element partElmt = doc.createElement("payload");
                Element rootElmt = doc.createElement("root");
                doc.appendChild(docElmt);
                docElmt.appendChild(partElmt);
                partElmt.appendChild(rootElmt);
                rootElmt.appendChild(doc.importNode(msgElmt, true));

                request.setMessage(docElmt);
                mex.setRequest(request);
            }
            try {
                mex.invokeBlocking();
            } catch (java.util.concurrent.TimeoutException te) {
                return Response.status(408).entity("The server timed out while processing the request.").build();
            }

            // TODO handle faults and failures

            if (mex.getResponse() == null) {
                return Response.status(204).build();
            } else {
                Response.ResponseBuilder b;
                if (mex.isInstantiatingResource())
                    b = Response.status(201).header("Location", _root + mex.getResource().getUrl());
                else
                    b = Response.status(200);
                return b.entity(unwrapResponse(mex.getResponse().getMessage()))
                        .type("application/xml")
                        .build();
            }
        }
        else return Response.status(405).header("Allow", _resource.methods()).build();
    }

    private String unwrapResponse(Element resp) {
        Element partElmt = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(resp));
        Element unwrapped = DOMUtils.getFirstChildElement(partElmt);
        if (unwrapped == null) return partElmt.getTextContent();
        else return DOMUtils.domToString(unwrapped);
    }

    @GET @Produces("application/xml") @Path("{sub : .*}")
    public Response getSub() {
        return get();
    }

    @POST @Consumes("application/xml")
    public Response post(String content) {
        try {
            Element msgElmt = null;
            if (content.length() > 0) msgElmt = DOMUtils.stringToDOM(content);
            return post(msgElmt);
        } catch (Exception e) {
            return Response.status(400).entity("Couldn't parse XML request.").type("text/plain").build();
        }
    }

    @POST @Consumes("application/xml") @Path("{sub : .*}")
    public Response postSub(String content) {
        try {
            Element msgElmt = null;
            if (content.length() > 0) msgElmt = DOMUtils.stringToDOM(content);
            return post(msgElmt);
        } catch (Exception e) {
            return Response.status(400).entity("Couldn't parse XML request.").type("text/plain").build();
        }
    }

    @POST @Consumes("application/x-www-form-urlencoded")
    public Response postEnc(MultivaluedMap<String, String> formParams) {
        return post(formToXml(formParams));
    }

    @POST @Consumes("application/x-www-form-urlencoded") @Path("{sub : .*}")
    public Response postSubEnc(MultivaluedMap<String, String> formParams) {
        return post(formToXml(formParams));
    }

    private Element formToXml(MultivaluedMap<String, String> formParams) {
        Document doc = DOMUtils.newDocument();
        Element form = doc.createElement("form");
        doc.appendChild(form);
        for (Map.Entry<String, List<String>> param : formParams.entrySet()) {
            if (param.getValue().size() == 1) {
                Element k = doc.createElement(param.getKey());
                k.setTextContent(param.getValue().get(0));
                form.appendChild(k);
            } else {
                // TODO support Rails encoding conventions as well
                Element container = doc.createElement(pluralize(param.getKey()));
                for (String s : param.getValue()) {
                    Element e = doc.createElement(param.getKey());
                    e.setTextContent(s);
                    container.appendChild(e);
                }
            }
        }
        return form;
    }

    private String pluralize(String s) {
        // Doing it the lazy way for now TODO get all pluralization rules from Rails
        return s + "s";
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

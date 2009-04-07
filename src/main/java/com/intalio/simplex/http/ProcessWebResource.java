/*
 * Simplex, lightweight SimPEL server
 * Copyright (C) 2008-2009  Intalio, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.intalio.simplex.http;

import com.intalio.simplex.embed.ServerLifecycle;
import com.intalio.simplex.http.datam.FEJOML;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.RESTInMessageExchange;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.GUID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.ws.rs.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class ProcessWebResource {

    private EngineWebResource.ResourceDesc _resource;
    private static ServerLifecycle _serverLifecyle;
    private String _root;
    private HashMap<String,String> _params;

    public ProcessWebResource(EngineWebResource.ResourceDesc resource, ServerLifecycle serverLifecyle,
                              String root, HashMap<String,String> params) {
        _resource = resource;
        _serverLifecyle = serverLifecyle;
        _root = root;
        _params = params;
    }

    @GET @Produces("application/xml")
    public Response get() {
        if (_resource.get) {
            RESTInMessageExchange mex = _serverLifecyle.getServer().createMessageExchange(
                    _resource.toResource("GET"), new GUID().toString());
            for (Map.Entry<String, String> param : _params.entrySet())
                mex.setParameter(param.getKey(), param.getValue());

            try {
                mex.invokeBlocking();
            } catch (java.util.concurrent.TimeoutException te) {
                return Response.status(408).entity("The server timed out while processing the request.").build();
            }

            if (mex.getResponse() == null) {
                return Response.status(204).build();
            } else {
                return Response.status(200)
                        .entity(DOMUtils.domToString(unwrapResponse(mex.getResponse().getMessage())))
                        .header("Location", _root+mex.getResource().getUrl())
                        .build();
            }
        }
        else return Response.status(405).header("Allow", _resource.methods()).build();
    }

    public Response post(Element msgElmt, String targetFormat) {
        if (_resource.post) {
            RESTInMessageExchange mex = _serverLifecyle.getServer().createMessageExchange(
                    _resource.toResource("POST"), new GUID().toString());
            Message request = mex.createMessage(null);
            if (msgElmt != null) {
                // TODO support for http headers and parameters as additional parts
                Document doc = DOMUtils.newDocument();
                Element docElmt = doc.createElement("document");
                Element partElmt = doc.createElement("payload");
                // For some reason, this sometimes ends up in assignment as a node with no local name if not ns'd
                Element rootElmt = doc.createElementNS(null, "root");
                doc.appendChild(docElmt);
                docElmt.appendChild(partElmt);
                partElmt.appendChild(rootElmt);
                rootElmt.appendChild(doc.importNode(msgElmt, true));

                request.setMessage(docElmt);
                mex.setRequest(request);
            }
            for (Map.Entry<String, String> param : _params.entrySet())
                mex.setParameter(param.getKey(), param.getValue());
            try {
                mex.invokeBlocking();
            } catch (java.util.concurrent.TimeoutException te) {
                return Response.status(408).entity("The server timed out while processing the request.").build();
            }

            // TODO handle faults and failures

            boolean hasResponse = mex.getResponse() != null && mex.getResponse().getMessage() != null;
            Response.ResponseBuilder b;
            if (mex.isInstantiatingResource()) {
                b = Response.status(201).header("Location", _root + mex.getResource().getUrl());
            } else {
                if (hasResponse) b = Response.status(200);
                else b = Response.status(204);
            }

            if (hasResponse)
                b.entity(FEJOML.fromXML(unwrapResponse(mex.getResponse().getMessage()), targetFormat));

            return b.build();
        }
        else return Response.status(405).header("Allow", _resource.methods()).build();
    }

    private Node unwrapResponse(Element resp) {
        Element partElmt = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(resp));
        Element unwrapped = DOMUtils.getFirstChildElement(partElmt);
        if (unwrapped == null)
            return partElmt.getOwnerDocument().createTextNode(partElmt.getTextContent());
        else return unwrapped;
    }

    @GET @Path("{sub : .*}")
    @Produces("application/xml")
    public Response getSub() {
        return get();
    }

    @POST
    @Consumes("application/xml") @Produces("application/xml")
    public Response post(String content) {
        try {
            Element msgElmt = null;
            if (content.length() > 0) msgElmt = DOMUtils.stringToDOM(content);
            return post(msgElmt, FEJOML.XML);
        } catch (Exception e) {
            return Response.status(400).entity("Couldn't parse XML request.").type("text/plain").build();
        }
    }

    @POST @Path("{sub : .*}")
    @Consumes("application/xml") @Produces("application/xml")
    public Response postSub(String content) {
        try {
            Element msgElmt = null;
            if (content.length() > 0) msgElmt = DOMUtils.stringToDOM(content);
            return post(msgElmt, FEJOML.XML);
        } catch (Exception e) {
            return Response.status(400).entity("Couldn't parse XML request.").type("text/plain").build();
        }
    }

    @POST
    @Consumes("application/x-www-form-urlencoded") @Produces("application/xml")
    public Response postEnc(MultivaluedMap<String, String> formParams) {
        return post(FEJOML.formToXML(formParams), FEJOML.XML);
    }

    @POST @Path("{sub : .*}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/xml")
    public Response postSubEnc(MultivaluedMap<String, String> formParams) {
        return post(FEJOML.formToXML(formParams), FEJOML.XML);
    }

    @GET @Produces("application/xhtml+xml")
    public String getXHTML() {
        StringBuffer res = new StringBuffer();
        res.append(HttpUtil.htmlHeader("Process Page"));
        res.append("<p>A process answers to this resource, accepted methods are " + _resource.methods()
                + ". The consumed content type is " + _resource.contentType + "</p>");        
        res.append(HttpUtil.htmlFooter());
        return res.toString();
    }
}

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
import com.intalio.simplex.lifecycle.StandaloneLifecycle;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.uri.UriTemplate;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.ode.bpel.iapi.Resource;
import org.apache.ode.utils.DOMUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.regex.MatchResult;
import java.net.URI;

@Path("/")
public class EngineWebResource {

    private static Server _server;
    private static ServerLifecycle _serverLifecyle;

    //    private HashMap<String,QName> _services = new HashMap<String, QName>();
    private static ConcurrentHashMap<UriTemplate,ResourceDesc> _engineResources;

    @GET @Produces("application/xhtml+xml")
    public String getXHTML() {
        StringBuffer res = new StringBuffer();
        res.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><body>");
        res.append("<p>List of deployed processes:</p>");
        res.append("<ul>");
        for (ResourceDesc r : _engineResources.values()) {
            String p = "/"+r.resourcePath;
            res.append("<li><a href=\"").append(p).append("\">").append(p).append("</a>");
            res.append("<span id=\"method\">").append(r.methods()).append("</span>");
            res.append("<span id=\"content-type\">").append(r.contentType).append("</span>");
            res.append("</li>");
        }
        res.append("</ul>");
        res.append("</body></html>");
        return res.toString();
    }

    @GET @Produces("application/xml")
    public String getXML() {
        Document doc = DOMUtils.newDocument();
        Element root = doc.createElement("resources");
        doc.appendChild(root);
        for (ResourceDesc r : _engineResources.values()) {
            Element pelmt = doc.createElement("resource");
            pelmt.setAttribute("methods", r.methods());
            pelmt.setAttribute("contentType", r.contentType);
            pelmt.setTextContent("/"+r.resourcePath);
            root.appendChild(pelmt);
        }
        return DOMUtils.domToString(doc);
    }

    @Path("{subpath}")
    public ProcessWebResource buildProcessResource(@javax.ws.rs.core.Context UriInfo subpath) {
        Object[] rdesc = findResource(subpath.getRequestUri().getPath());
        if (rdesc == null) throw new NotFoundException("No known resource at this location.");
        else if (((ResourceDesc)rdesc[0]).removed) throw new WebApplicationException(Response.status(410)
                .entity("The resource isn't available anymore.").type("text/plain").build());
        else {
            return new ProcessWebResource((ResourceDesc)rdesc[0], _serverLifecyle,
                    getRoot(subpath.getRequestUri()), (HashMap<String,String>)rdesc[1]);
        }
    }

    private Object[] findResource(String url) {
        String surl = stripSlashes(url);
        for (Map.Entry<UriTemplate, ResourceDesc> resourceDesc : _engineResources.entrySet()) {
            MatchResult mr;
            if ((mr = resourceDesc.getKey().getPattern().match(surl)) != null) {
                HashMap<String,String> params = new HashMap<String,String>();
                List<String> vars = resourceDesc.getKey().getTemplateVariables();
                for (int m = 0; m < mr.groupCount(); m++)
                    params.put(vars.get(m), mr.group(m+1));
                return new Object[] { resourceDesc.getValue(), params };
            }
        }
        return null;
    }

    private static String stripSlashes(String sl) {
        int start = sl.charAt(0) == '/' ? 1 : 0;
        int end = sl.charAt(sl.length()-1) == '/' ? sl.length() - 1 : sl.length();
        return sl.substring(start, end);
    }

    public static void registerResource(Resource resource) {
        String nonSlashed = stripSlashes(resource.getUrl());
        ResourceDesc desc = _engineResources.get(new UriTemplate(nonSlashed));
        if (desc == null) {
            desc = new ResourceDesc();
            desc.resourcePath = nonSlashed;
            _engineResources.put(new UriTemplate(nonSlashed), desc);
        } else {
            desc.removed = false;
        }
        desc.enable(resource.getMethod());
    }

    public static void unregisterResource(Resource resource) {
        ResourceDesc rdesc = _engineResources.get(new UriTemplate(stripSlashes(resource.getUrl())));
        rdesc.removed = true;
        // TODO eventually cleanup removed resources after a while
    }

    public static void startRestfulServer(ServerLifecycle serverLifecyle) {
        _serverLifecyle = serverLifecyle;
        _engineResources = new ConcurrentHashMap<UriTemplate,ResourceDesc>();

        ServletHolder sh = new ServletHolder(ServletContainer.class);
        sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", "com.intalio.simplex.http");
        ServletHandler shh = new ServletHandler();
        shh.addServletWithMapping(sh, "/*");

        _server = new Server(3434);

        if (_serverLifecyle instanceof StandaloneLifecycle) {
            // Serving files in the script directory in addition to Jersey resources
            ResourceHandler rh = new ResourceHandler();
            rh.setResourceBase(((StandaloneLifecycle)_serverLifecyle).getScriptsDir().getAbsolutePath());

            HandlerList hl = new HandlerList();
            hl.setHandlers(new Handler[] { rh, shh });
            _server.addHandler(hl);
        } else {
            _server.addHandler(shh);
        }

        try {
            _server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopRestfulServer() {
        try {
            _server.stop();
            _server = null;
            _serverLifecyle = null;
            _engineResources = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class ResourceDesc {
        String resourcePath;
        String contentType;
        boolean get;
        boolean post;
        boolean put;
        boolean delete;
        boolean removed;

        public Resource toResource(String method) {
            return new Resource("/"+resourcePath, contentType, method);
        }

        public void enable(String method) {
            if ("GET".equalsIgnoreCase(method)) get = true;
            else if ("POST".equalsIgnoreCase(method)) post = true;
            else if ("PUT".equalsIgnoreCase(method)) put = true;
            else if ("DELETE".equalsIgnoreCase(method)) delete = true;
        }
        public String methods() {
            StringBuffer m = new StringBuffer();
            if (get) m.append("GET");
            if (post) {
                if (m.length() > 0) m.append(",");
                m.append("POST");
            }
            if (put) {
                if (m.length() > 0) m.append(",");
                m.append("PUT");
            }
            if (delete) {
                if (m.length() > 0) m.append(",");
                m.append("DELETE");
            }
            return m.toString();
        }
    }

    private String getRoot(URI uri) {
        return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
    }
}

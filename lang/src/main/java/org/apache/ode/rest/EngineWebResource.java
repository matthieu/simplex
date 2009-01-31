package org.apache.ode.rest;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response;

import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.Server;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.bpel.iapi.Resource;
import org.apache.ode.embed.ServerLifecycle;

import java.util.concurrent.ConcurrentHashMap;

@Path("/")
public class EngineWebResource {

    private static Server _server;
    private static ServerLifecycle _serverLifecyle;

    //    private HashMap<String,QName> _services = new HashMap<String, QName>();
    private static ConcurrentHashMap<String,ResourceDesc> _engineResources;

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
        // TODO This should be able to match based on a pattern
        ResourceDesc rdesc = _engineResources.get(stripSlashes(subpath.getPath()));
        if (rdesc == null) throw new NotFoundException("No known resource at this location.");
        else if (rdesc.removed) throw new WebApplicationException(Response.status(410)
                .entity("The resource isn't available anymore.").type("text/plain").build());
        else {
            String base = subpath.getBaseUri().toString();
            return new ProcessWebResource(rdesc, _serverLifecyle, base.substring(0, base.length()-1));
        }
    }

    private static String stripSlashes(String sl) {
        int start = sl.charAt(0) == '/' ? 1 : 0;
        int end = sl.charAt(sl.length()-1) == '/' ? sl.length() - 1 : sl.length();
        return sl.substring(start, end);
    }

    public static void registerResource(Resource resource) {
        String nonSlashed = stripSlashes(resource.getUrl());
        ResourceDesc desc = _engineResources.get(nonSlashed);
        if (desc == null) {
            desc = new ResourceDesc();
            desc.resourcePath = nonSlashed;
            _engineResources.put(nonSlashed, desc);
        }
        desc.enable(resource.getMethod());
    }

    public static void unregisterResource(Resource resource) {
        ResourceDesc rdesc = _engineResources.get(stripSlashes(resource.getUrl()));
        rdesc.removed = true;
        // TODO eventually cleanup removed resources after a while
    }

    public static void startRestfulServer(ServerLifecycle serverLifecyle) {
        _serverLifecyle = serverLifecyle;
        _engineResources = new ConcurrentHashMap<String,ResourceDesc>();
        ServletHolder sh = new ServletHolder(ServletContainer.class);

        sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", "org.apache.ode.rest");

        _server = new Server(3434);
        Context context = new Context(_server, "/", Context.SESSIONS);
        context.addServlet(sh, "/*");
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
}

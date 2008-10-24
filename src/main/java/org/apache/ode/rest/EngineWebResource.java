package org.apache.ode.rest;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.UriInfo;

import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.Server;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.bpel.iapi.Resource;
import org.apache.ode.embed.ServerLifecycle;

import java.util.concurrent.ConcurrentLinkedQueue;

@Path("/")
public class EngineWebResource {

    private static Server _server;
    private static ServerLifecycle _serverLifecyle;

    //    private HashMap<String,QName> _services = new HashMap<String, QName>();
    private static ConcurrentLinkedQueue<Resource> _engineResources = new ConcurrentLinkedQueue<Resource>();

    @GET @Produces("application/xhtml+xml")
    public String getXHTML() {
        StringBuffer res = new StringBuffer();
        res.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><body>");
        res.append("<p>List of deployed processes:</p>");
        res.append("<ul>");
        for (Resource r : _engineResources) {
            res.append("<li><a href=\"").append(r.getUrl()).append("\">").append(r.getUrl()).append("</a>");
            res.append("<span id=\"method\">").append(r.getMethod()).append("</span>");
            res.append("<span id=\"content-type\">").append(r.getContentType()).append("</span>");
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
        for (Resource r : _engineResources) {
            Element pelmt = doc.createElement("resource");
            pelmt.setAttribute("method", r.getMethod());
            pelmt.setAttribute("contentType", r.getContentType());
            pelmt.setTextContent(r.getUrl());
            root.appendChild(pelmt);
        }
        return DOMUtils.domToString(doc);
    }

    @Path("{subpath}")
    public ProcessWebResource buildProcessResource(@javax.ws.rs.core.Context UriInfo subpath) {
        for (Resource engineResource : _engineResources) {
            // TODO This should be able to match based on a pattern
            if (stripSlashes(subpath.getPath()).equals(stripSlashes(engineResource.getUrl()))) 
                return new ProcessWebResource(engineResource, _serverLifecyle);
        }
        throw new NotFoundException("Resource " + subpath.getPath() + " is unknown.");        
    }

    private String stripSlashes(String sl) {
        int start = sl.charAt(0) == '/' ? 1 : 0;
        int end = sl.charAt(sl.length()-1) == '/' ? sl.length() - 1 : sl.length();
        return sl.substring(start, end);
    }

    public static void registerResource(Resource resource) {
        _engineResources.add(resource);
    }

    public static void startRestfulServer(ServerLifecycle serverLifecyle) {
        _serverLifecyle = serverLifecyle;
        ServletHolder sh = new ServletHolder(ServletContainer.class);

        sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                "com.sun.jersey.api.core.PackagesResourceConfig");
        sh.setInitParameter("com.sun.jersey.config.property.packages", "org.apache.ode.rest");

        _server = new Server(3033);
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

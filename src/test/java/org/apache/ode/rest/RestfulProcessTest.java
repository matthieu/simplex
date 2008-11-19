package org.apache.ode.rest;

import junit.framework.TestCase;
import org.apache.ode.EmbeddedServer;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.config.ClientConfig;

public class RestfulProcessTest extends TestCase {

    private static final String HELLO_WORLD =
            "process HelloWorld {\n" +
            "   helloRes = resource(\"/hello\"); \n" +
            "   receive(helloRes) { |name|\n" +
            "       helloName = \"Hello \" + name;\n" +
            "       reply(helloName);\n" +
            "   }\n" +
            "}";

    public void testProcessGet() throws Exception {
        EmbeddedServer server = new EmbeddedServer();
        server.options.makeRestful();
        server.start();

        server.deploy(HELLO_WORLD);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3033");
        String processes = wr.path("/").accept("application/xml").get(String.class);
        System.out.println("=> " + processes);
        assertTrue(processes.indexOf("/hello") > 0);

        // Check different representations (html, xml)
        // Links to instance list search, process start url, process start form
    }
}

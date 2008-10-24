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
            "   receive(myPl, helloOp) { |msgIn|\n" +
            "       msgOut = msgIn + \" World\";\n" +
            "       reply(msgOut);\n" +
            "   }\n" +
            "}";

    public void testProcessGet() throws Exception {
        EmbeddedServer server = new EmbeddedServer();
        server.options.makeRestful();
        server.start();

        server.deploy(HELLO_WORLD);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3033/ode");
        String processes = wr.path("/").accept("application/xml").get(String.class);
        assertTrue(processes.indexOf("HelloWorld") > 0);

        System.out.println("=> " + processes);
        Element processesElmt = DOMUtils.stringToDOM(processes);
        NodeList processNL = processesElmt.getElementsByTagName("process");
        assertTrue(processNL.getLength() > 0);
        assertEquals("process", processNL.item(0).getNodeName());

        String processUrl = processNL.item(0).getTextContent();

        String process = wr.path(processUrl).accept("application/xml").get(String.class);
        System.out.println("=> " + process);

        Thread.sleep(10000);

        // Check different representations (html, xml)
        // Links to instance list search, process start url, process start form
    }
}

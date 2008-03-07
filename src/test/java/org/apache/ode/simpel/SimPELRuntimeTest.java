package org.apache.ode.simpel;

import junit.framework.TestCase;
import org.apache.ode.EmbeddedServer;
import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELRuntimeTest extends TestCase {

    private static final String HELLO_WORLD =
            "process HelloWorld {\n" +
            "  receive(my_pl, hello_op) { |msg_in|\n" +
            "    msg_out = msg_in + \" World\";\n" +
            "    reply(msg_out);\n" +
            "  }\n" +
            "}";

    private static final String POLITE_HELLO_WORLD =
            "process HelloWorld {\n" +
            "  receive(my_pl, hello_op) { |info|\n" +
            "    msg_out = msg_in + \" World\";\n" +
            "    reply(msg_out);\n" +
            "  }\n" +
            "}";

    public void testHelloWorldComplete() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.start();
        server.deploy(HELLO_WORLD);

        Document doc = DOMUtils.newDocument();
        Element wrapper = doc.createElementNS("http://ode.apache.org/simpel/1.0/definition/HelloWorld", "hello_opRequest");
        wrapper.setTextContent("Hello");

        Element result = server.sendMessage("my_pl", "hello_op", wrapper);
        assertTrue(DOMUtils.domToString(result).indexOf("Hello World") > 0);
    }

}

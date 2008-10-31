package org.apache.ode.simpel;

import org.apache.ode.EmbeddedServer;
import junit.framework.TestCase;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.Response;

public class RestfulSimPELTest extends TestCase {

    private static final String HELLO_WORLD =
            "process HelloWorld {\n" +
            "   helloRes = resource(\"/hello\"); \n" +
            "   receive(helloRes) { |name|\n" +
            "       helloName = \"Hello \" + name;\n" +
            "       reply(helloName);\n" +
            "   }\n" +
            "}";

    public void testRestfulHelloWorld() throws Exception {
        EmbeddedServer server = new  EmbeddedServer();
        server.options.makeRestful();
        server.start();
        server.deploy(HELLO_WORLD);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3033/hello");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class, "<simpelWrapper xmlns=\"http://ode.apache.org/simpel/1.0/definition/HelloWorld\">foo</simpelWrapper>");
        System.out.println("=> " + resp.getEntity(String.class));
        System.out.println("loc " + resp.getMetadata().get("Location"));
    }
}

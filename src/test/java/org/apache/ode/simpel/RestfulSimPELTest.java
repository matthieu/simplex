package org.apache.ode.simpel;

import org.apache.ode.EmbeddedServer;
import org.apache.ode.Descriptor;
import junit.framework.TestCase;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.Response;
import java.util.regex.Pattern;

public class RestfulSimPELTest extends TestCase {

    private static final String HELLO_WORLD =
            "process HelloWorld {\n" +
            "   receive(self) { |name|\n" +
            "       helloName = \"Hello \" + name;\n" +
            "       reply(helloName);\n" +
            "   }\n" +
            "}";

    public void testRestfulHelloWorld() throws Exception {
        EmbeddedServer server = new EmbeddedServer();
        server.start();
        Descriptor desc = new Descriptor();
        desc.setAddress("/hello");
        server.deploy(HELLO_WORLD, desc);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/hello");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class, "<simpelWrapper xmlns=\"http://ode.apache.org/simpel/1.0/definition/HelloWorld\">foo</simpelWrapper>");
        String response = resp.getEntity(String.class);
        System.out.println("=> " + response);
        assertTrue(response.indexOf("Hello foo") > 0);
        assertTrue(resp.getMetadata().get("Location").get(0), resp.getMetadata().get("Location").get(0).matches("/hello/[0-9]*"));
        System.out.println("loc " + resp.getMetadata().get("Location"));
        server.stop();
    }

    private static final String COUNTER =
            "process Counter {\n" +
            "   counter = receive(self); \n" +
            "   reply(counter, self); \n" +
            "   value = resource(\"/value\"); \n" +
            "   inc = resource(\"/inc\"); \n" +
            "   dec = resource(\"/dec\"); \n" +
            "   scope { \n" +
            "       while(counter>0) { \n" +
            "           wait(\"PT1S\"); \n" +
            "       } \n" +
            "   } onQuery(self) {\n" +
            "       links = <counter></counter>; \n" +
            "       links.increment = inc; \n" +
            "       links.decrement = dec; \n" +
            "       links.value = value; \n" +
            "       reply(links); \n" +
            "   } onQuery(value) {\n" +
            "       reply(counter); \n" +
            "   } onReceive(dec) {\n" +
            "       counter = counter - 1; \n" +
            "       reply(counter); \n" +
            "   } \n" +
            "}";

    public void testCounter() throws Exception {
        EmbeddedServer server = new EmbeddedServer();
        server.start();
        Descriptor desc = new Descriptor();
        desc.setAddress("/counter");
        server.deploy(COUNTER, desc);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/counter");
        ClientResponse createResponse = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class, "<simpelWrapper xmlns=\"http://ode.apache.org/simpel/1.0/definition/Counter\">5</simpelWrapper>");
        String response = createResponse.getEntity(String.class);
        System.out.println("=> " + response);
        System.out.println("=> " + createResponse.getMetadata().get("Location").get(0));

        String location = createResponse.getMetadata().get("Location").get(0);
        WebResource instance = c.resource(location);
        ClientResponse queryResponse = instance.path("/").type("application/xml").get(ClientResponse.class);
        System.out.println("=> " + queryResponse.getStatus());
        System.out.println("=> " + queryResponse.getEntity(String.class));

        server.stop();
    }
}

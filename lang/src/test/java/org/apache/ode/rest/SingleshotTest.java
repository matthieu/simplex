package org.apache.ode.rest;

import org.apache.ode.EmbeddedServer;
import org.apache.ode.Descriptor;
import org.junit.Ignore;
import junit.framework.TestCase;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

/**
 * For now this test requires a running instance of Singleshot, otherwise it will fail.
 */
@Ignore
public class SingleshotTest extends TestCase {

    EmbeddedServer server;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        server = new EmbeddedServer();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        server.stop();
    }

    private static final String TASK_CREATOR =
            "shotBase = \"http://localhost:3000\" \n" +
            "process TaskCreator { \n" +
            "   receive(self) { |task| \n" +
            "       task.headers.basicAuth.login = \"mriou\"; \n" +
            "       task.headers.basicAuth.password = \"secret\"; \n" +
            "       resp = request(shotBase + \"/tasks\", \"POST\", task); \n" +
            "       reply(resp); \n" +
            "   }\n" +
            "}";

    public void testCreateTask() throws Exception {
        server.start();
        Descriptor desc = new Descriptor();
        desc.setAddress("/taskcreator");
        server.deploy(TASK_CREATOR, desc);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/taskcreator");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class, "<task><title>SimPEL Task</title>" +
                        "<description>Test task created from a SimPEL process.</description>" +
                        "<owner>mriou</owner>" +
                        "<data>&#x7B;&#x7D;</data></task>"); // Opening and closing curly literals escaped
        String response = resp.getEntity(String.class);
        System.out.println(response);
    }
}

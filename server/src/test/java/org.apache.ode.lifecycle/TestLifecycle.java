package org.apache.ode.lifecycle;

import junit.framework.TestCase;
import org.apache.ode.StandaloneServer;

import java.io.File;
import java.io.FileOutputStream;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

public class TestLifecycle extends TestCase {

    private static final String HELLO_WORLD =
            "processConfig.address = \"/hello\";\n" +

            "process HelloWorld { \n" +
            "   receive(self) { |name| \n" +
            "       helloXml = <hello>{\"Hello \" + name}</hello>; \n" +
            "       reply(helloXml); \n" +
            "   }\n" +
            "}";

    public void testFSDeployUndeploy() throws Exception {
        String rootDir = new File(getClass().getClassLoader().getResource("marker").getFile()).getParent();
        StandaloneServer.main(new String[] { rootDir });

        File pfile = new File(rootDir, "scripts/helloworld.simpel");
        FileOutputStream fos = new FileOutputStream(pfile);
        fos.write(HELLO_WORLD.getBytes());
        fos.close();

        Thread.sleep(3000);
        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/hello");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class, "<wrapper>foo</wrapper>");
        String response = resp.getEntity(String.class);
        System.out.println("=> " + response);
        assertTrue(response.indexOf("Hello foo") > 0);
        assertTrue(resp.getMetadata().get("Location").get(0), resp.getMetadata().get("Location").get(0).matches(".*/hello/[0-9]*"));
        assertTrue(resp.getStatus() == 201);

        pfile.delete();
        Thread.sleep(3000);

        assertTrue(!new File(rootDir, "work/helloworld.cbp").exists());
    }

}
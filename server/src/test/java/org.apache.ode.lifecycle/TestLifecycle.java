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
    private File rootDir;
    private StandaloneServer server;

    @Override
    protected void setUp() throws Exception {
        rootDir = new File(new File(getClass().getClassLoader().getResource("marker").getFile()).getParent());
        server = new StandaloneServer(rootDir);
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
    }

    private static final String HELLO_WORLD =
            "processConfig.address = \"/hello\";\n" +

            "process HelloWorld { \n" +
            "   receive(self) { |name| \n" +
            "       helloXml = <hello>{\"Hello \" + name}</hello>; \n" +
            "       reply(helloXml); \n" +
            "   }\n" +
            "}";

    public void testFSDeployUndeploy() throws Exception {
        writeProcessFile("helloworld.simpel", HELLO_WORLD);
        Thread.sleep(5000);
        
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

        new File(rootDir, "scripts/helloworld.simpel").delete();
        Thread.sleep(300);

        assertTrue(!new File(rootDir, "work/helloworld.cbp").exists());
        new File(rootDir, "work/helloworld.cbp").delete();
    }

    public void testDeployEmpty() throws Exception {
        writeProcessFile("empty.simpel", "");

        File cbp = new File(rootDir, "work/empty.cbp");
        assertTrue(cbp.exists());
        assertTrue(cbp.length() == 0);

        writeProcessFile("empty.simpel", HELLO_WORLD);
        cbp = new File(rootDir, "work/empty.cbp");
        assertTrue(cbp.exists());
        assertTrue(cbp.length() > 0);

        new File(rootDir, "scripts/empty.simpel").delete();
        cbp.delete();
    }

    public void testLoadInProcess() throws Exception {
        writeProcessFile("foo.js", "var foo = 2;\n");
        writeProcessFile("load-js.simpel", "load('foo.js');\n" + HELLO_WORLD);

        Thread.sleep(2000);
        File cbp = new File(rootDir, "work/load-js.cbp");
        assertTrue(cbp.exists());
        assertTrue(cbp.length() > 0);

        new File(rootDir, "scripts/load-js.simpel").delete();
        cbp.delete();
    }

    private void writeProcessFile(String filename, String content) throws Exception {
        // speeding things up
        ScriptBasedStore.POLLING_FREQ = 50;

        File pfile = new File(rootDir, "scripts/" + filename);
        FileOutputStream fos = new FileOutputStream(pfile);
        fos.write(content.getBytes());
        fos.close();
        Thread.sleep(1000);
    }

}
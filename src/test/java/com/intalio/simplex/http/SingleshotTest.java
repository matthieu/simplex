/*
 * Simplex, lightweight SimPEL server
 * Copyright (C) 2008-2009  Intalio, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.intalio.simplex.http;

import com.intalio.simpel.Descriptor;
import com.intalio.simplex.EmbeddedServer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import junit.framework.TestCase;
import org.junit.Ignore;

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

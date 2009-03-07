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

import com.intalio.simplex.EmbeddedServer;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import junit.framework.TestCase;
import org.junit.Ignore;

@Ignore
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
        server.start();

        server.deploy(HELLO_WORLD);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434");
        String processes = wr.path("/").accept("application/xml").get(String.class);
        System.out.println("=> " + processes);
        assertTrue(processes.indexOf("/hello") > 0);

        // Check different representations (html, xml)
        // Links to instance list search, process start url, process start form
        server.stop();
    }
}

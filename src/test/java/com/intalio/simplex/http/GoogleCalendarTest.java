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

import junit.framework.TestCase;
import com.intalio.simplex.EmbeddedServer;
import com.intalio.simpel.Descriptor;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

public class GoogleCalendarTest extends TestCase {

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

    private static final String CREATE_ENTRY =
            "googleLoginUrl = \"https://www.google.com/accounts/ClientLogin\"; \n" +
            "process CalendarEntry { \n" +
            "   receive(self) { |entry| \n" +
            "     creds = <creds><source>intalio-simpeltest-0.1</source><service>cl</service></creds>;\n" +
            "     creds.Email = entry.email;\n" +
            "     creds.Passwd = entry.password;\n" +
            "     creds.header.ContentEncoding = \"application/x-www-form-urlencoded\"; \n" +

            "     tokenResp = request(\"post\", googleLoginUrl);\n" +
            "     if (tokenResp.header.status == 403) {\n" +
            "         error = <loginError>403</loginError>;\n" +
            "         reply(error);\n" +
            "     } else {\n" +
            "         token = tokenResp.Auth;\n" +
            "         reply(token);\n" +
            "     }" +
            "   }\n" +
            "}";

    public void testCreateEntry() throws Exception {
        server.start();
        Descriptor desc = new Descriptor();
        desc.setAddress("/calentry");
        server.deploy(CREATE_ENTRY, desc);

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/calentry");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class,
                        "<task><title>SimPEL Task</title>" +
                        "<description>Test task created from a SimPEL process.</description>" +
                        "<email>simpeltests@gmail.com</email>" +
                        "<password>!sekr33t</password>");
        String response = resp.getEntity(String.class);
        System.out.println(response);
    }

}

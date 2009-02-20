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
import org.junit.Ignore;

@Ignore
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
            "processConfig.inMem = true;\n" +
            "processConfig.address = \"/calentry\";\n" +
            "googleLoginUrl = \"https://www.google.com/accounts/ClientLogin\"; \n" +
            "googleCalUrl = \"http://www.google.com/calendar/feeds/default/private/full\"; \n" +

            "function extractToken(resp) { \n" +
            "  return resp.split(\"=\")[3]; \n" +
            "} \n" +
            "function extractSessionId(url) { \n" +
            "  return url.split(\"gsessionid=\")[1]; \n" +
            "} \n" +

            "function createCalEntry() { \n" +
            "  return <entry xmlns='http://www.w3.org/2005/Atom'\n" +
                    "    xmlns:gd='http://schemas.google.com/g/2005'>\n" +
                    "  <category scheme='http://schemas.google.com/g/2005#kind'\n" +
                    "    term='http://schemas.google.com/g/2005#event'></category>\n" +
                    "  <title type='text'>Review stuff</title>\n" +
                    "  <content type='text'>See how well you can do your stuff.</content>\n" +
                    "  <gd:transparency\n" +
                    "    value='http://schemas.google.com/g/2005#event.opaque'>\n" +
                    "  </gd:transparency>\n" +
                    "  <gd:eventStatus\n" +
                    "    value='http://schemas.google.com/g/2005#event.confirmed'>\n" +
                    "  </gd:eventStatus>\n" +
                    "  <gd:where valueString='Your Office'></gd:where>\n" +
                    "  <gd:when startTime='2009-03-17T15:00:00.000Z'\n" +
                    "    endTime='2009-03-17T17:00:00.000Z'></gd:when>\n" +
                    "</entry>; \n" +
            "} \n" +

            "process CalendarEntry { \n" +
            "   receive(self) { |entry| \n" +
            "     creds = <creds><source>intalio-simpeltest-0.1</source><service>cl</service></creds>;\n" +
            "     creds.Email = <Email>{entry.email.text()}</Email>;\n" +
            "     creds.Passwd = <Passwd>{entry.password.text()}</Passwd>;\n" +
            "     creds.headers.Content_Type = \"application/x-www-form-urlencoded\"; \n" +

            "     tokenResp = request(googleLoginUrl, \"post\", creds);\n" +
            "     if (tokenResp.header.Status == 403) {\n" +
            "         error = <loginError>403</loginError>;\n" +
            "         reply(error);\n" +
            "     } else {\n" +
            "         token = extractToken(tokenResp);\n" +
            "         entryReq = createCalEntry();\n" +
            "         entryReq.headers.Authorization = \"GoogleLogin auth=\" + token;\n" +
            "         entryReq.headers.GData_Version = 2;\n" +
            "         entryReq.headers.Content_Type = \"application/atom+xml\"; \n" +
            "         entryResp = request(googleCalUrl, \"post\", entryReq);\n" +
            "         reply(entryResp);\n" +
            "     }" +
            "   }\n" +
            "}";

    public void testCreateEntry() throws Exception {
        server.start();
        if (server.deploy(CREATE_ENTRY) == null) fail("Deployment error.");

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        WebResource wr = c.resource("http://localhost:3434/calentry");
        ClientResponse resp = wr.path("/").accept("application/xml").type("application/xml")
                .post(ClientResponse.class,
                        "<task><title>SimPEL Task</title>" +
                        "<description>Test task created from a SimPEL process.</description>" +
                        "<email>simpeltests@gmail.com</email>" +
                        "<password>!sekr33t</password></task>");
        String response = resp.getEntity(String.class);
        System.out.println(response);
    }

}

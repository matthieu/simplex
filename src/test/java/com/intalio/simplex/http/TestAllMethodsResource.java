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

import org.apache.ode.utils.DOMUtils;
import org.w3c.dom.Element;

import javax.ws.rs.*;

@Path("/gppd")
public class TestAllMethodsResource {

    @GET
    @Produces("application/xml")
    public String get() {
        return "<get>GET</get>";
    }

    @POST
    @Consumes("application/xml")
    @Produces("application/xml")
    public String post(String input) {
        String out = "";
        try {
            Element root = DOMUtils.stringToDOM(input);
            out = root.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<post>POST" + out + "</post>";
    }

    @PUT
    @Consumes("application/xml")
    @Produces("application/xml")
    public String put(String input) {
        String out = "";
        try {
            Element root = DOMUtils.stringToDOM(input);
            out = root.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<put>PUT" + out + "</put>";
    }

    @DELETE
    public void delete() {
        System.out.println("*** DELETE ***");
    }

}

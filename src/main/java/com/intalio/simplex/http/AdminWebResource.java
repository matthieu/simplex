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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.bpel.iapi.ProcessStore;
import org.apache.ode.bpel.iapi.ProcessConf;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.xml.namespace.QName;
import java.text.MessageFormat;

@Path("admin")
public class AdminWebResource {

    private static ProcessStore _store;

    // No easy to initialize that stuff with other references without going static
    public static void init(ProcessStore store) {
        _store = store;
    }
    
    @GET @Produces("application/xhtml+xml")
    public String getHTML() {
        StringBuffer res = new StringBuffer();
        res.append(HttpUtil.htmlHeader("Administration"));
        if (_store.getProcesses().size() == 0) {
            res.append("<p>").append("No processes have been deployed yet. Just drop your " +
                    "SimPEL files in the script folder.").append("</p>");
        } else {
            res.append("<p>").append("The following processes are deployed:").append("</p>");
            for (QName pname : _store.getProcesses()) {
                res.append("<li>").append(pname.getLocalPart()).append("</li>");
            }
        }
        res.append(HttpUtil.htmlFooter());
        return res.toString();
    }

    @GET @Produces("application/xml")
    public String getXML() {
        Document doc = DOMUtils.newDocument();
        Element root = doc.createElement("resources");
        doc.appendChild(root);
        return DOMUtils.domToString(doc);
    }

}

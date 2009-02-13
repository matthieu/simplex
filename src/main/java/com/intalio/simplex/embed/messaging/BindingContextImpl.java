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

package com.intalio.simplex.embed.messaging;

import com.intalio.simplex.Options;
import org.apache.ode.il.epr.URLEndpoint;
import org.apache.ode.il.epr.WSDL11Endpoint;
import com.intalio.simplex.http.EngineWebResource;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.Namespaces;
import org.apache.ode.bpel.iapi.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.wsdl.PortType;
import javax.xml.namespace.QName;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class BindingContextImpl implements BindingContext {

    private Options _options;

    public BindingContextImpl(Options options) {
        _options = options;
    }

    public EndpointReference activateMyRoleEndpoint(QName qName, Endpoint endpoint) {
        Document doc = DOMUtils.newDocument();
        Element serviceElmt = doc.createElementNS(Namespaces.WSDL_11, "service");
        serviceElmt.setAttribute("name", endpoint.serviceName.getLocalPart());
        serviceElmt.setAttribute("targetNamespace", endpoint.serviceName.getNamespaceURI());
        Element portElmt = doc.createElementNS(Namespaces.WSDL_11, "port");
        portElmt.setAttribute("name", endpoint.portName);
        portElmt.setAttribute("xmlns:bindns", "http://ode.apache.org/simpel/1.0/endpoint");
        portElmt.setAttribute("binding", "bindns:simpelUnknownBinding");
        doc.adoptNode(serviceElmt);
        serviceElmt.appendChild(portElmt);
        WSDL11Endpoint epr = new WSDL11Endpoint();
        epr.set(serviceElmt);
        return epr;
    }

    public void deactivateMyRoleEndpoint(Endpoint endpoint) {
        // Nothing needed here
    }

    public void activateProvidedResource(Resource resource) {
        EngineWebResource.registerResource(resource);
    }

    public void deactivateProvidedResource(Resource resource) {
        EngineWebResource.unregisterResource(resource);
    }

    public PartnerRoleChannel createPartnerRoleChannel(QName qName, PortType portType, Endpoint endpoint) {
        // TODO implement me
        return new PartnerRoleChannelImpl();
    }

    private class PartnerRoleChannelImpl implements PartnerRoleChannel {
        public EndpointReference getInitialEndpointReference() {
            return new URLEndpoint();
        }

        public void close() {
        }
    }
}

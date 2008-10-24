package org.apache.ode.embed.messaging;

import org.apache.ode.bpel.iapi.*;
import org.apache.ode.il.epr.WSDL11Endpoint;
import org.apache.ode.il.epr.URLEndpoint;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.Namespaces;
import org.apache.ode.Options;
import org.apache.ode.rest.EngineWebResource;
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

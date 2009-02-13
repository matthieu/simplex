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
package com.intalio.simplex;

import com.intalio.simpel.Descriptor;
import com.intalio.simplex.embed.ServerLifecycle;
import org.apache.log4j.Logger;
import org.apache.ode.bpel.iapi.InvocationStyle;
import org.apache.ode.bpel.iapi.Message;
import org.apache.ode.bpel.iapi.MessageExchange;
import org.apache.ode.bpel.iapi.MyRoleMessageExchange;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.GUID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.concurrent.Future;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class EmbeddedServer {
    private static final Logger __log = Logger.getLogger(EmbeddedServer.class);

    public Options options;
    protected ServerLifecycle _resources;

    public EmbeddedServer() {
        this.options = new Options();
    }

    public void start() {
        start(options);
    }

    public void start(Options options) {
        this.options = options;
        _resources = new ServerLifecycle(options);
        _resources.start();
    }

    public void stop() {
        _resources.clean();
    }

    public Collection<QName> deploy(String process) {
        return deploy(process, new Descriptor());
    }

    public Collection<QName> deploy(String process, Descriptor desc) {
        return _resources.getStore().deploy(process, desc);
    }

    public void undeploy(String dir) {
        throw new UnsupportedOperationException();
    }

    public Element sendMessage(String partnerLink, String operation, Element message) {
        String messageId = new GUID().toString();
        MyRoleMessageExchange odeMex = _resources.getServer().createMessageExchange(InvocationStyle.UNRELIABLE,
                new QName("http://ode.apache.org/simpel/1.0/endpoint", partnerLink), operation, "" + messageId);
        odeMex.setTimeout(100000000);
        // TODO see what kind of exceptions we should throw from here
        if (odeMex.getOperation() == null)
            throw new RuntimeException("Call to " + partnerLink + "." + operation + " was not routable.");

        Document doc = DOMUtils.newDocument();
        Element msgEl = doc.createElementNS(null, "message");
        doc.appendChild(msgEl);
        Element part = doc.createElement(operation+"Request"); // default part
        msgEl.appendChild(part);
        part.appendChild(doc.importNode(message, true));

        Message odeRequest = odeMex.createMessage(null);
        odeRequest.setMessage(msgEl);

        odeMex.setRequest(odeRequest);
        try {
            odeMex.invokeBlocking();
        } catch (java.util.concurrent.TimeoutException te) {
            throw new RuntimeException("Call to " + partnerLink + "." + operation + " timed out!", te);         
        }

        if (odeMex.getAckType() == MessageExchange.AckType.FAILURE)
            throw new RuntimeException("Failure:" + odeMex.getFailureType() + ": " + odeMex.getFaultExplanation());

        if (odeMex.getOperation().getOutput() != null) {
            return DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(odeMex.getResponse().getMessage()));
        }
        return null;
    }

    public Future sendMessageAsync(String processName, Element message) {
        throw new UnsupportedOperationException();
    }


}

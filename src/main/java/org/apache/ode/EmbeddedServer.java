/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode;

import org.apache.log4j.Logger;
import org.apache.ode.bpel.iapi.*;
import org.apache.ode.embed.ServerResources;
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

    protected Options _options;
    protected ServerResources _resources;

    public void start() {
        start(new Options());
    }

    public void start(Options options) {
        _options = options;
        _resources = new ServerResources(options);
    }

    public void stop() {
    }

    public Collection<QName> deploy(String process) {
        return _resources.getStore().deploy(process);
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

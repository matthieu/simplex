package org.apache.ode.embed.messaging;

import org.apache.ode.bpel.iapi.*;
import org.apache.ode.embed.MessageSender;
import org.apache.ode.utils.DOMUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.wsdl.Operation;
import javax.wsdl.Fault;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.Set;
import java.util.HashSet;
import java.io.UnsupportedEncodingException;

import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

public class MessageExchangeContextImpl implements MessageExchangeContext {

    private static final Logger __log = Logger.getLogger(MessageExchangeContextImpl.class);

    MessageSender _sender;

    public MessageExchangeContextImpl(MessageSender sender) {
        _sender  = sender;
    }

    public void invokePartnerUnreliable(PartnerRoleMessageExchange partnerMex) throws ContextException {
        if (_sender == null) {
            partnerMex.replyWithFailure(MessageExchange.FailureType.ABORTED,
                    "No sender configured, can't send the message.", partnerMex.getRequest().getMessage());
            __log.warn("No sender configured, can't send the message:\n"
                    + DOMUtils.domToString(partnerMex.getRequest().getMessage()));
        }

        Operation invokedOp = partnerMex.getPortType().getOperation(partnerMex.getOperationName(), null, null);
        try {
            // We're placing ourselves in the doc/lit case for now, assuming a single part with a single root element
            Node response = _sender.send(partnerMex.getPortType().getQName().getLocalPart(),
                    invokedOp.getName(), unwrapToPayload(partnerMex.getRequest().getMessage()));

            if (invokedOp.getOutput() != null) {
                Document responseDoc = DOMUtils.newDocument();
                Element messageElmt = responseDoc.createElement("message");
                responseDoc.appendChild(messageElmt);
                // Pretty hard to get the part name huh?
                String partName = (String) invokedOp.getOutput().getMessage().getParts().keySet().iterator().next();
                Element partElmt = responseDoc.createElement(partName);
                messageElmt.appendChild(partElmt);
                // TODO same thing, simpel only wrapping
                QName elmtName = ((Part)invokedOp.getOutput().getMessage().getParts().values().iterator().next()).getElementName();
                Element partRootElmt = responseDoc.createElementNS(elmtName.getNamespaceURI(), elmtName.getLocalPart());
                partElmt.appendChild(partRootElmt);
                if (response != null) partRootElmt.appendChild(responseDoc.importNode(response, true));

                Message responseMsg = partnerMex.createMessage(invokedOp.getOutput().getMessage().getQName());
                responseMsg.setMessage(messageElmt);
                partnerMex.reply(responseMsg);
            } else {
                partnerMex.replyOneWayOk();
            }
        } catch (RuntimeException re) {
            __log.warn("The service called threw a runtime exception:\n"
                    + DOMUtils.domToString(partnerMex.getRequest().getMessage()), re);
            // Runtimes are considered failures
            partnerMex.replyWithFailure(MessageExchange.FailureType.COMMUNICATION_ERROR,
                    "The service called threw a runtime exception: " + re.toString(),
                    partnerMex.getRequest().getMessage());
        } catch (Exception e) {
            __log.warn("The service called threw a checked exception:\n"
                    + DOMUtils.domToString(partnerMex.getRequest().getMessage()), e);
            // checked exceptions are considered faults
            Fault fault = invokedOp.getFault(e.getClass().getName());
            Message faultMsg = partnerMex.createMessage(fault.getMessage().getQName());
            Document faultDoc = DOMUtils.newDocument();
            Element faultElmt = faultDoc.createElement("exception");
            faultElmt.setTextContent(e.getMessage());
            faultMsg.setMessage(faultElmt);
            partnerMex.replyWithFault(new QName(
                    partnerMex.getPortType().getQName().getNamespaceURI(), fault.getName()), faultMsg);
        }
    }

    public void invokePartnerReliable(PartnerRoleMessageExchange partnerRoleMessageExchange) throws ContextException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void invokePartnerTransacted(PartnerRoleMessageExchange partnerRoleMessageExchange) throws ContextException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void invokeRestful(RESTOutMessageExchange restOutMessageExchange) throws ContextException {
        Resource res = restOutMessageExchange.getTargetResource();

        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);

        ClientResponse resp;
        WebResource.Builder wr = c.resource(res.getUrl()).path("/").accept(res.getContentType()).type(res.getContentType());
        if (restOutMessageExchange.getRequest() != null) {
            Element payload = restOutMessageExchange.getRequest().getMessage();
            handleOutHeaders(payload, wr);
            resp = wr.method(res.getMethod().toUpperCase(), ClientResponse.class,
                    DOMUtils.domToString(unwrapToPayload(payload)));
        } else resp = wr.method(res.getMethod().toUpperCase(), ClientResponse.class);

        if (resp.getStatus() == 204) {
            restOutMessageExchange.replyOneWayOk();
            return;
        }

        String response = resp.getEntity(String.class);

        int responseType = isFaultOrFailure(resp.getStatus());
        if (responseType > 0) {
            faultFromHttpStatus(resp.getStatus(), response, restOutMessageExchange);
            return;
        }
        if (responseType < 0) {
            fail(res.getUrl(), "http" + resp.getStatus(), "Failing with HTTP response code "
                    + resp.getStatus(), restOutMessageExchange);
            return;
        }

        // TODO allow POST over simple form url-encoded
        Element responseXML = null;
        if (response != null && response.trim().length() > 0) {
            try {
                responseXML = DOMUtils.stringToDOM(response);
            } catch (Exception e) {
                fail(res.getUrl(), "parseError", "Response couldn't be parsed: " + response, restOutMessageExchange);
                return;
            }
        }

        // Prepare the message
        Document odeMsg = DOMUtils.newDocument();
        Element odeMsgEl = odeMsg.createElementNS(null, "message");
        odeMsg.appendChild(odeMsgEl);
        Element partElmt = odeMsg.createElement("payload");
        odeMsgEl.appendChild(partElmt);
        Element methodElmt = odeMsg.createElement(res.getMethod() + "Response");
        partElmt.appendChild(methodElmt);
        if (responseXML != null)
            methodElmt.appendChild(odeMsg.adoptNode(responseXML));

        // Copy headers
        if (resp.getStatus() == 201) {
            Element loc = odeMsg.createElement("Location");
            loc.setTextContent(resp.getMetadata().getFirst("Location"));
            withHeaders(methodElmt).appendChild(loc);
        }
        Element status = odeMsg.createElement("Status");
        status.setTextContent(""+resp.getStatus());
        withHeaders(methodElmt).appendChild(status);

        Message responseMsg = restOutMessageExchange.createMessage(null);
        responseMsg.setMessage(odeMsgEl);
        restOutMessageExchange.reply(responseMsg);
    }

    public void cancel(PartnerRoleMessageExchange partnerRoleMessageExchange) throws ContextException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onMyRoleMessageExchangeStateChanged(MyRoleMessageExchange myRoleMessageExchange) throws BpelEngineException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<InvocationStyle> getSupportedInvocationStyle(PartnerRoleChannel partnerRoleChannel, EndpointReference endpointReference) {
        HashSet<InvocationStyle> styles = new HashSet<InvocationStyle>();
        // TODO only unreliable for now, we might want to do transactional at a point
        styles.add(InvocationStyle.UNRELIABLE);
        return styles;
    }

    private Node unwrapToPayload(Element message) {
        Element root = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(message));
        // TODO this assumption only works with SimPEL, in the general case we could have a NodeList
        // and should therefore send the whole part element
        Node payload;
        if (DOMUtils.getFirstChildElement(root) != null)
            payload = DOMUtils.getFirstChildElement(root);
        else {
            Document doc = DOMUtils.newDocument();
            payload = doc.createTextNode(DOMUtils.getTextContent(root));
        }
        return payload;
    }

    private void faultFromHttpStatus(int s, String response, RESTOutMessageExchange mex) {
        QName faultName = new QName(null, "http401");
        Document odeMsg = DOMUtils.newDocument();
        Element odeMsgEl = odeMsg.createElementNS(null, "message");
        odeMsg.appendChild(odeMsgEl);
        Element partElmt = odeMsg.createElement("payload");
        odeMsgEl.appendChild(partElmt);
        Element methodElmt = odeMsg.createElementNS(faultName.getNamespaceURI(), faultName.getLocalPart());
        partElmt.appendChild(methodElmt);
        methodElmt.setTextContent(response);

        Message responseMsg = mex.createMessage(null);
        responseMsg.setMessage(odeMsgEl);
        mex.replyWithFault(faultName, responseMsg);
    }

    private void fail(String calledUrl, String errElmt, String text, RESTOutMessageExchange mex) {
        Document doc = DOMUtils.newDocument();
        Element failureElmt = doc.createElement(errElmt);
        failureElmt.setTextContent(text);
        String fullMsg = "Request to " + calledUrl + " failed. " + text;
        __log.debug(fullMsg);
        mex.replyWithFailure(MessageExchange.FailureType.FORMAT_ERROR, fullMsg, failureElmt);
    }

    private void handleOutHeaders(Element msg, WebResource.Builder wr) {
        Element root = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(msg));
        Node headers = DOMUtils.findChildByName(root, new QName(null, "headers"));
        if (headers != null) {
            NodeList headerElmts = headers.getChildNodes();
            for (int m = 0; m < headerElmts.getLength(); m++) {
                Node n = headerElmts.item(m);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    if (n.getNodeName().equals("basicAuth")) {
                        Element login = DOMUtils.findChildByName((Element) n, new QName(null, "login"));
                        Element password = DOMUtils.findChildByName((Element) n, new QName(null, "password"));
                        if (login != null && password != null) {
                            // TODO rely on Jersey basic auth once 1.0.2 is released
                            try {
                                byte[] unencoded = (login.getTextContent() + ":" + password.getTextContent()).getBytes("UTF-8");
                                String credString = Base64.encode(unencoded);
                                String authHeader = "Basic " + credString;
                                wr.header("authorization", authHeader);
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    private Element withHeaders(Element element) {
        Element res = DOMUtils.findChildByName(element, new QName(null, "headers"));
        if (res == null) {
            Element headers = element.getOwnerDocument().createElement("headers");
            element.appendChild(headers);
            res = headers;
        }
        return res;
    }

    /**
     * @param s, the status code to test, must be in [400, 600[
     * @return 1 if fault, -1 if failure, 0 if success
     */
    public static int isFaultOrFailure(int s) {
        if (s < 100 || s >= 600)
            throw new IllegalArgumentException("Status-Code must be in interval [400,600]");

        if (s == 500 || s == 501 || s == 502 || s == 505
                || s == 400 || s == 402 || s == 403 || s == 404 || s == 405 || s == 406
                || s == 409 || s == 410 || s == 412 || s == 413 || s == 414 || s == 415
                || s == 411 || s == 416 || s == 417) {
            return 1;
        } else if (s == 503 || s == 504 || s == 401 || s == 407 || s == 408) {
            return -1;
        } else {
            return 0;
        }
    }
    
}

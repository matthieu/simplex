package org.apache.ode.embed;

import org.w3c.dom.Node;

public interface MessageSender {

    Node send(String recipient, String operation, Node message);
}

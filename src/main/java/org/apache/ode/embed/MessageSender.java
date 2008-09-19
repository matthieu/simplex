package org.apache.ode.embed;

import org.w3c.dom.Element;

public interface MessageSender {

    Element send(String recipient, String operation, Element elmt);
}

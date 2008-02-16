package org.apache.ode.simpel.wsdl;

import javax.wsdl.Part;
import javax.xml.namespace.QName;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELPart extends SimPELWSDLElement implements Part {
    public void setName(String s) {
    }

    public String getName() {
        return "payload";
    }

    public void setElementName(QName qName) {
    }

    public QName getElementName() {
        return new QName("http://ode.apache.org/simpel/1.0/definition", "simpelWrapper");
    }

    public void setTypeName(QName qName) {
    }

    public QName getTypeName() {
        return null;
    }
}

package org.apache.ode.simpel.wsdl;

import javax.wsdl.Output;
import javax.wsdl.Message;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELOutput extends SimPELWSDLElement implements Output {
    private String name;
    public SimPELOutput(String name) {
        this.name = name;
    }
    public void setName(String s) {
        this.name = s;
    }
    public String getName() {
        return name;
    }
    public void setMessage(Message message) {
    }
    public Message getMessage() {
        return null;
    }
}

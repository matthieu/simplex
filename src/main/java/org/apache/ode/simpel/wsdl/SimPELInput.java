package org.apache.ode.simpel.wsdl;

import javax.wsdl.Input;
import javax.wsdl.Message;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class SimPELInput extends SimPELWSDLElement implements Input {
    private String name;
    private SimPELMessage _message = new SimPELMessage();

    public SimPELInput(String name) {
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
        return _message;
    }
}

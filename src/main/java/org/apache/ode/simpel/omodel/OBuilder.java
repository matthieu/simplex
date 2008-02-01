package org.apache.ode.simpel.omodel;

import org.apache.log4j.Logger;
import org.apache.ode.bpel.compiler.BaseCompiler;
import org.apache.ode.bpel.compiler.bom.Bpel20QNames;
import org.apache.ode.bpel.o.*;
import org.apache.ode.utils.GUID;
import org.w3c.dom.Element;

import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class OBuilder extends BaseCompiler {
    private static final Logger __log = Logger.getLogger(OBuilder.class);
    private static final String SIMPEL_NS = "http://ode.apache.org/simpel/1.0/definition";

    private HashMap<String,String> namespaces = new HashMap<String,String>();
    private HashMap<String,OPartnerLink> partnerLinks = new HashMap<String,OPartnerLink>();
    private boolean firstReceive = false;

    public OScope buildProcess(String prefix, String name) {
        _oprocess = new OProcess(Bpel20QNames.NS_WSBPEL2_0_FINAL_EXEC);
        _oprocess.guid = new GUID().toString();
        _oprocess.constants = makeConstants();
        _oprocess.compileDate = new Date();
        if (namespaces.get(prefix) == null) _oprocess.targetNamespace = SIMPEL_NS;
        else _oprocess.targetNamespace = namespaces.get(prefix);

        OScope processScope = new OScope(_oprocess, null);
        processScope.name = "__PROCESS_SCOPE:" + name;
        _oprocess.procesScope = processScope;
        return processScope;
    }

    public OPickReceive buildReceive(OActivity parent, String partnerLink, String operation) {
        OPickReceive receive = new OPickReceive(_oprocess, parent);

        OPickReceive.OnMessage onMessage = new OPickReceive.OnMessage(_oprocess);
        onMessage.partnerLink = resolvePartnerLink(partnerLink);
        onMessage.operation = new SimPELOperation(operation);

        if (firstReceive) {
            firstReceive = false;
            onMessage.partnerLink.addCreateInstanceOperation(onMessage.operation);
            receive.createInstanceFlag = true;
        }

        onMessage.activity = new OEmpty(_oprocess, receive);
        receive.onMessages.add(onMessage);

        return receive;
    }

    public OSequence buildSequence(OActivity parent) {
        OSequence seq = new OSequence(_oprocess, parent);
        return seq;
    }

    public void setBlockParam(OActivity blockActivity, String varName) {
        if (blockActivity == null) {
            __log.warn("Can't set block parameter on activity null.");
            return;
        }
        if (blockActivity instanceof OPickReceive) {
            ((OPickReceive)blockActivity).onMessages.get(0).variable = new OScope.Variable(_oprocess, null);
        } else throw new RuntimeException("Can't set block parameter on activity " + blockActivity);
    }

    public OProcess getProcess() {
        return _oprocess;
    }

    private OPartnerLink resolvePartnerLink(String name) {
        OPartnerLink resolved = partnerLinks.get(name);
        if (resolved == null) {
            resolved = new OPartnerLink(_oprocess);
            resolved.name = name;
        }
        return resolved;
    }

    protected String getBpwsNamespace() {
        return "http://ode.apache.org/simpel/1.0";
    }

    public class SimPELOperation implements Operation {
        private String name;

        public SimPELOperation(String name) {
            this.name = name;
        }

        public void setName(String s) {
            this.name = s;
        }

        public String getName() {
            return name;
        }

        public void setInput(Input input) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Input getInput() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setOutput(Output output) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Output getOutput() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addFault(Fault fault) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Fault getFault(String s) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Fault removeFault(String s) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Map getFaults() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setStyle(OperationType operationType) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public OperationType getStyle() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setParameterOrdering(List list) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public List getParameterOrdering() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setUndefined(boolean b) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isUndefined() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setDocumentationElement(Element element) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Element getDocumentationElement() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setExtensionAttribute(QName qName, Object o) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getExtensionAttribute(QName qName) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Map getExtensionAttributes() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public List getNativeAttributeNames() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addExtensibilityElement(ExtensibilityElement extensibilityElement) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public ExtensibilityElement removeExtensibilityElement(ExtensibilityElement extensibilityElement) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public List getExtensibilityElements() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}

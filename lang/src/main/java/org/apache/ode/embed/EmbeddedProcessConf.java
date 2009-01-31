package org.apache.ode.embed;

import org.apache.ode.bpel.rtrep.v2.OProcess;
import org.apache.ode.bpel.rapi.Serializer;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.apache.ode.bpel.rapi.PartnerLinkModel;
import org.apache.ode.bpel.iapi.ProcessState;
import org.apache.ode.bpel.iapi.Endpoint;
import org.apache.ode.bpel.iapi.EndpointReference;
import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.bpel.evt.BpelEvent;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.wsdl.Definition;
import java.io.*;
import java.net.URI;
import java.util.*;

public class EmbeddedProcessConf implements ProcessConf {
    private static final String SIMPEL_ENDPOINT_NS = "http://ode.apache.org/simpel/1.0/endpoint";

    private OProcess _oprocess;


    public EmbeddedProcessConf(OProcess _oprocess) {
        this._oprocess = _oprocess;
    }

    public QName getProcessId() {
        return new QName(_oprocess.getQName().getNamespaceURI(),
                _oprocess.getQName().getLocalPart()+"-"+getVersion());
    }

    public QName getType() {
        return _oprocess.getQName();
    }

    public long getVersion() {
        // TODO implement versioning
        return 0;
    }

    public boolean isTransient() {
        return true;
    }

    public boolean isRestful() {
        return getProvideEndpoints().size() == 0;
    }

    public InputStream getCBPInputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer fileHeader = new Serializer(System.currentTimeMillis());
        try {
            fileHeader.writePModel(_oprocess, baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize compiled OProcess!", e);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public ProcessModel getProcessModel() {
        return _oprocess;
    }

    public String getBpelDocument() {
        throw new UnsupportedOperationException();
    }

    public URI getBaseURI() {
        throw new UnsupportedOperationException();
    }

    public Date getDeployDate() {
        throw new UnsupportedOperationException();
    }

    public ProcessState getState() {
        return ProcessState.ACTIVE;
    }

    public List<File> getFiles() {
        throw new UnsupportedOperationException();
    }

    public String getPackage() {
        throw new UnsupportedOperationException();
    }

    public Definition getDefinitionForService(QName qName) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Endpoint> getProvideEndpoints() {
        return defaultEndpoints(true);
    }

    public Map<String, Endpoint> getInvokeEndpoints() {
        return defaultEndpoints(false);
    }

    public boolean isEventEnabled(List<String> strings, BpelEvent.TYPE type) {
        return false;
    }

    private Map<String, Endpoint> defaultEndpoints(boolean myrole) {
        Map<String, Endpoint> res = new HashMap<String, Endpoint>();
        for (PartnerLinkModel partnerLink : _oprocess.getAllPartnerLinks()) {
            if (partnerLink.hasMyRole() && myrole || partnerLink.hasPartnerRole() && !myrole)
                res.put(partnerLink.getName(), new Endpoint(
                        new QName(SIMPEL_ENDPOINT_NS, partnerLink.getName()), "SimPELPort"));
        }
        return res;
    }

    public Map<QName, Node> getProcessProperties() {
        throw new UnsupportedOperationException();
    }

    public List<Element> getExtensionElement(QName qName) {
        return new ArrayList<Element>();
    }

    public Map<String, String> getEndpointProperties(EndpointReference endpointReference) {
        throw new UnsupportedOperationException();
    }

    public boolean isSharedService(QName qName) {
        return false;
    }

    public int getRuntimeVersion() {
        return 2;
    }
}

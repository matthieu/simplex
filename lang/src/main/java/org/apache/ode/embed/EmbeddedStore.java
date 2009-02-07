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

package org.apache.ode.embed;

import org.apache.ode.bpel.rtrep.v2.*;

import org.apache.ode.bpel.evt.BpelEvent;
import org.apache.ode.bpel.iapi.*;
import org.apache.ode.bpel.rapi.PartnerLinkModel;
import org.apache.ode.bpel.rapi.Serializer;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.apache.ode.simpel.SimPELCompiler;
import org.apache.ode.simpel.CompilationException;
import org.apache.ode.Descriptor;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * TODO In the ProcessStore and the ProcessConf interfaces, some methods are part of the contract
 * TODO with the runtime, others with the IL or the management API. Put some order in that mess.
 */
public class EmbeddedStore implements ProcessStore {

    protected SimPELCompiler _compiler = new SimPELCompiler();
    private ArrayList<ProcessStoreListener> _listeners = new ArrayList<ProcessStoreListener>();
    protected HashMap<QName, ProcessModel> _processes = new HashMap<QName, ProcessModel>();
    protected HashMap<QName, Descriptor> _descriptors = new HashMap<QName, Descriptor>();

    public Collection<QName> deploy(String processStr, Descriptor desc) {
        OProcess op = null;
        try {
            op = _compiler.compileProcess(processStr, desc);
        } catch (CompilationException e) {
            System.err.println("There were errors during the compilation of a SimPEL process:\n" + e.toString());
        }
        _processes.put(op.getQName(), op);
        _descriptors.put(op.getQName(), desc);

        fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.DEPLOYED, op.getQName(), null));        
        fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.ACTIVATED, op.getQName(), null));
        
        LinkedList<QName> ll = new LinkedList<QName>();
        ll.add(op.getQName());
        return ll;
    }

    protected void start() {
        // Nothing special to do
    }

    protected void fireEvent(ProcessStoreEvent pse) {
        for (ProcessStoreListener psl : _listeners) psl.onProcessStoreEvent(pse);
    }

    public List<QName> getProcesses() {
        return new LinkedList<QName>(_processes.keySet());
    }

    public ProcessConf getProcessConfiguration(QName processId) {
        return new EmbeddedProcessConf(_processes.get(processId), _descriptors.get(processId));
    }

    public void registerListener(ProcessStoreListener psl) {
        _listeners.add(psl);
    }

    public void unregisterListener(ProcessStoreListener psl) {
        _listeners.remove(psl);
    }

    // boilerplate

    public Collection<QName> deploy(File bpelFile) {
        // TODO come up with some sort of common deployment model
        throw new UnsupportedOperationException();
    }

    public Collection<QName> undeploy(File file) {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getPackages() {
        throw new UnsupportedOperationException();
    }

    public List<QName> listProcesses(String packageName) {
        throw new UnsupportedOperationException();
    }

    public void setProperty(QName pid, QName propName, String value) {
        throw new UnsupportedOperationException();
    }

    public void setProperty(QName pid, QName propName, Node value) {
        throw new UnsupportedOperationException();
    }

    public void setState(QName pid, ProcessState state) {
        throw new UnsupportedOperationException();
    }

    public void setRetiredPackage(String packageName, boolean retired) {
        // TODO Implement process retirement
        throw new UnsupportedOperationException();
    }

    public long getCurrentVersion() {
        return 0;
    }

}

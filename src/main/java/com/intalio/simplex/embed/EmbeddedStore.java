/*
 * Simplex, lightweight SimPEL server
 * Copyright (C) 2008-2009  Intalio, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.intalio.simplex.embed;

import com.intalio.simpel.CompilationException;
import com.intalio.simpel.Descriptor;
import com.intalio.simpel.SimPELCompiler;
import org.apache.ode.bpel.iapi.*;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.apache.ode.bpel.rtrep.v2.OProcess;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.File;
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
            System.err.println("There were errors during the compilation of a SimPEL process:\n" + e.getMessage());
            return null;
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

    protected void stop() {
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

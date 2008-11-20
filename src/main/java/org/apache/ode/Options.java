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
package org.apache.ode;

import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.ode.bpel.iapi.BpelEventListener;
import org.apache.ode.bpel.iapi.MessageExchangeContext;
import org.apache.ode.embed.MessageSender;

import java.io.File;
import java.util.Properties;
import java.util.List;

/**
 * Used to customize the behavior of the server in several sort of ways.
 */
public class Options {

    private OdeConfigProperties _odeProps;
    private Properties _backingProps;
    private List<BpelEventListener> _eventListeners;
    private boolean _enableRest = true;

    public Options() {
        _backingProps = new Properties();
        _odeProps = new OdeConfigProperties(_backingProps, "embed");
    }

    /** @return the maximum amoubt of threads used by ODE to process messages */
    public int getThreadPoolMaxSize() {
        return _odeProps.getThreadPoolMaxSize();
    }
    /** @param maxSize the maximum number of threads ODE will be allowed to use to process messages */
    public void setThreadPoolMaxSize(int maxSize) {
        _backingProps.setProperty(OdeConfigProperties.PROP_THREAD_POOL_SIZE, ""+maxSize);
    }

    /** @return whether dehydration is enabled. Dehydration allows processe descriptions to be lazy loaded in
     * memory to save some space if you have a lot of them. */
    public boolean isDehydrationEnabled() {
        return _odeProps.isDehydrationEnabled();
    }
    /** @param enable dehydration */
    public void setDehydrationEnabled(boolean enable) {
        _backingProps.setProperty(OdeConfigProperties.PROP_PROCESS_DEHYDRATION, ""+enable);
    }

    public String getDatabasePath() {
        return _backingProps.getProperty("database-path");
    }
    public void setDatabasePath(String path) {
        _backingProps.setProperty("database-path", path);
    }

    public List<BpelEventListener> getBpelEventListeners() {
        return _eventListeners;
    }
    public void getBpelEventListeners(List<BpelEventListener> listeners) {
        _eventListeners = listeners;
    }

    public MessageSender getMessageSender() {
        return (MessageSender) _backingProps.get(MessageSender.class.getName());
    }
    public void setMessageSender(MessageSender sender) {
        _backingProps.put(MessageSender.class.getName(), sender);
    }

    public Properties getProperties() {
        return _backingProps;
    }

    public boolean isRestful() {
        return _enableRest;
    }
}

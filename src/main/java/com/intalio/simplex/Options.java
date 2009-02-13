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
package com.intalio.simplex;

import com.intalio.simplex.embed.MessageSender;
import org.apache.ode.bpel.iapi.BpelEventListener;
import org.apache.ode.il.config.OdeConfigProperties;

import java.util.List;
import java.util.Properties;

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

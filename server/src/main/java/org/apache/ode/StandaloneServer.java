package org.apache.ode;

import org.apache.log4j.Logger;

import org.apache.ode.lifecycle.StandaloneLifecycle;

public class StandaloneServer {
    private static final Logger __log = Logger.getLogger(StandaloneServer.class);

    public Options options;
    protected StandaloneLifecycle _resources;

    public StandaloneServer() {
        this.options = new Options();
    }

    public void start() {
        start(options);
    }

    public void start(Options options) {
        this.options = options;
        _resources = new StandaloneLifecycle(options);
    }

    public void stop() {
        _resources.clean();
    }

    public static void main(String[] args) {
        StandaloneServer server = new StandaloneServer();
        server.start();
    }
}

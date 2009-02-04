package org.apache.ode;

import org.apache.log4j.Logger;

import org.apache.ode.lifecycle.StandaloneLifecycle;

import java.io.File;

public class StandaloneServer {
    private static final Logger __log = Logger.getLogger(StandaloneServer.class);

    public Options options;
    protected StandaloneLifecycle _resources;
    protected File _serverRoot;

    public StandaloneServer(File serverRoot) {
        this.options = new Options();
        _serverRoot = serverRoot;
    }

    public void start() {
        start(options);
    }

    public void start(Options options) {
        this.options = options;
        _resources = new StandaloneLifecycle(_serverRoot, options);
        _resources.start();
    }

    public void stop() {
        _resources.clean();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please pass the location of the root directory as first parameter.");
            return;
        }

        File serverRoot = new File(args[0]);
        if (!serverRoot.exists()) {
            System.err.println("The provided root directory doesn't seem to exist. It's going to be hard to start.");
            return;
        }

        StandaloneServer server = new StandaloneServer(serverRoot);
        server.start();
    }
}

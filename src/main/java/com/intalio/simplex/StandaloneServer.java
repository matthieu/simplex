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

import com.intalio.simplex.lifecycle.StandaloneLifecycle;
import org.apache.log4j.Logger;

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

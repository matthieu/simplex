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

package com.intalio.simplex.lifecycle;

import com.intalio.simplex.Options;
import com.intalio.simplex.embed.ServerLifecycle;
import org.apache.log4j.Logger;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.ode.il.dbutil.Database;

import java.io.*;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StandaloneLifecycle extends ServerLifecycle {
    
    private static final Logger __log = Logger.getLogger(StandaloneLifecycle.class);

    protected File _scriptsDir;
    protected File _workDir;
    protected File _derbyDir;
    protected File _libDir;

    public StandaloneLifecycle(File serverRoot, Options options) {
        super(options);

        String sysScriptsDir = System.getProperty("simplex.script.dir");
        _scriptsDir = sysScriptsDir != null ? new File(sysScriptsDir) : new File(serverRoot, "scripts");
        if (!_scriptsDir.exists()) _scriptsDir.mkdirs();

        String sysWorkDir = System.getProperty("simplex.work.dir");
        _workDir = sysWorkDir != null ? new File(sysWorkDir) : new File(serverRoot, "work");
        if (!_workDir.exists()) _workDir.mkdirs();

        String sysDerbyDir = System.getProperty("simplex.db.dir");
        _derbyDir = sysDerbyDir != null ? new File(sysDerbyDir) : new File(_workDir, "db");

        _libDir = new File(serverRoot, "lib");
        unzipPublicHtml();
    }

    protected void initDataSource() {
        try {
            Properties p = new Properties();
            if (!_derbyDir.exists()) {
                p.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=false)");
                p.put(OdeConfigProperties.PROP_DB_EMBEDDED_CREATE, "true");
            }
            p.put(OdeConfigProperties.PROP_DB_EMBEDDED_NAME, "db");

            OdeConfigProperties odeConfig = new OdeConfigProperties(p, "");
            _db = new Database(odeConfig);
            _db.setTransactionManager(_txMgr);
            _db.setWorkRoot(_workDir);

            _db.start();
            _ds = _db.getDataSource();
        } catch (Exception ex) {
            throw new RuntimeException("Database initialization failed.", ex);
        }
    }

    protected void initDAO() {
        try {
            _daoCF = _db.createDaoCF();
        } catch (Exception ex) {
            throw new RuntimeException("Database connection configuration failed.", ex);
        }
    }

    protected void initProcessStore() {
        _store = new ScriptBasedStore(_scriptsDir, _workDir);
        _store.registerListener(new ProcessStoreListenerImpl());
    }

    public File getScriptsDir() {
        return _scriptsDir;
    }

    public File getWorkDir() {
        return _workDir;
    }

    private void unzipPublicHtml() {
        File[] fileList = _libDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith("simplex-public-html");
                }
            });
        if (fileList != null) {
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(fileList[0]));
                ZipEntry entry;
                // Processing the package
                while((entry = zis.getNextEntry()) != null) {
                    if(entry.isDirectory()) {
                        new File(_workDir, entry.getName()).mkdir();
                        continue;
                    }

                    File destFile = new File(_workDir, entry.getName());
                    if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();

                    copyInputStream(zis, new BufferedOutputStream(new FileOutputStream(destFile)));
                }
                zis.close();
            } catch (IOException e) {
                throw new RuntimeException("Unzipping public HTML resources failed.", e);
            }
        }
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        out.close();
    }

}

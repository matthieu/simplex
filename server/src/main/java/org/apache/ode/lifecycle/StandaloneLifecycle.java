package org.apache.ode.lifecycle;

import org.apache.ode.embed.ServerLifecycle;
import org.apache.ode.Options;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.il.dbutil.Database;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.*;

public class StandaloneLifecycle extends ServerLifecycle {
    
    private static final Logger __log = Logger.getLogger(StandaloneLifecycle.class);

    protected File _scriptsDir;
    protected File _workDir;
    protected File _derbyDir;

    public StandaloneLifecycle(File serverRoot, Options options) {
        super(options);

        String sysScriptsDir = System.getProperty("simplex.script.dir");
        _scriptsDir = sysScriptsDir != null ? new File(sysScriptsDir) : new File(serverRoot, "scripts");
        if (!_scriptsDir.exists()) _scriptsDir.mkdirs();

        String sysWorkDir = System.getProperty("simplex.work.dir");
        _workDir = sysWorkDir != null ? new File(sysWorkDir) : new File(serverRoot, "work");
        if (!_workDir.exists()) _workDir.mkdirs();

        String sysDerbyZipDir = System.getProperty("simplex.db.dir");
        _derbyDir = sysDerbyZipDir != null ? new File(sysDerbyZipDir) : new File(_workDir, "db");
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

}

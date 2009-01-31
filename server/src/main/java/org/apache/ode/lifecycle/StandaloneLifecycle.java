package org.apache.ode.lifecycle;

import org.apache.ode.embed.ServerLifecycle;
import org.apache.ode.Options;
import org.apache.ode.bpel.iapi.EndpointReferenceContext;
import org.apache.ode.il.dbutil.Database;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.io.File;

public class StandaloneLifecycle extends ServerLifecycle {
    private static final Logger __log = Logger.getLogger(StandaloneLifecycle.class);

    protected String _scriptsDir;
    protected String _workDir;
    protected String _derbyZip;

    public StandaloneLifecycle(Options options) {
        super(options);
        _scriptsDir = System.getProperty("simpel.scripts");
    }

    protected void initDataSource() {
        prepareDerby();
        Properties odeProps = new Properties();
        odeProps.setProperty(OdeConfigProperties.PROP_DB_EMBEDDED_NAME, "derby");

        OdeConfigProperties odeConfig = new OdeConfigProperties(odeProps, "");
        _db = new Database(odeConfig);
        _db.setTransactionManager(_txMgr);
        _db.setWorkRoot(new File(_workDir));

        try {
            _db.start();
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
    }

    protected void prepareDerby() {
        // Unzips Derby in the working directory if it isn't already
    }
}

package org.apache.ode.lifecycle;

import org.apache.ode.embed.EmbeddedStore;
import org.apache.ode.embed.EmbeddedProcessConf;
import org.apache.ode.bpel.rapi.Serializer;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.apache.ode.bpel.iapi.ProcessStoreEvent;
import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.simpel.CompilationException;
import org.apache.ode.Descriptor;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptBasedStore extends EmbeddedStore {
    private static final Logger __log = Logger.getLogger(ScriptBasedStore.class);

    private File _scriptsDir;
    private File _workDir;

    public ScriptBasedStore(File scriptsDir, File workDir) {
        _scriptsDir = scriptsDir;
        _workDir = workDir;
        Thread poller = new Thread(new ScriptPoller());
        poller.setDaemon(true);
        poller.start();
    }

    private class ScriptPoller implements Runnable {
        private final FileFilter _scriptsFilter = new FileFilter() {
            public boolean accept(File path) {
                return path.getName().endsWith(".simpel") && path.isFile();
            }
        };
        private final FileFilter _cbpFilter = new FileFilter() {
            public boolean accept(File path) {
                return path.getName().endsWith(".cbp") && path.isFile();
            }
        };

        public void run() {
            while (true) {
                try {
                    List<File> scripts = listFilesRecursively(_scriptsDir, _scriptsFilter);
                    List<File> cbps = listFilesRecursively(_workDir, _cbpFilter);

                    // This whole mumbo jumbo is just about populating these lists
                    List<File> newer = new ArrayList<File>();
                    List<File> unknown = new ArrayList<File>();
                    List<File> removed = new ArrayList<File>(cbps);

                    for (File script : scripts) {
                        String scriptRelative = noExt(relativePath(_scriptsDir, script));

                        boolean found = false;
                        for (File cbp : cbps) {
                            String cbpRelative = noExt(relativePath(_workDir, cbp));
                            if (scriptRelative.equals(cbpRelative)) {
                                found = true;
                                removed.remove(cbp);
                                if (cbp.lastModified() < script.lastModified())
                                    newer.add(script);
                            }
                        }

                        if (!found) unknown.add(script);
                    }

                    ArrayList<File> toRebuild = new ArrayList<File>(unknown);
                    toRebuild.addAll(newer);
                    for (File p : toRebuild) {
                        ProcessModel oprocess = compileProcess(p);
                        fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.DEPLOYED, oprocess.getQName(), null));
                        fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.ACTIVATED, oprocess.getQName(), null));
                    }
                    for (File p : removed) {
                        Serializer ser = new Serializer(new FileInputStream(p));
                        ProcessModel oprocess = ser.readPModel();
                        fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.UNDEPLOYED, oprocess.getQName(), null));
                        _processes.remove(oprocess.getQName());
                        p.delete();
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // whatever
                        e.printStackTrace();
                    }
                } catch (Throwable t) {
                    __log.info(t.toString() + "\nDeployment aborted.", t);
                }
            }
        }

        private ProcessModel compileProcess(File pfile) throws IOException {
            File targetCbp = new File(_workDir, noExt(relativePath(_scriptsDir, pfile)) + ".cbp");
            targetCbp.getParentFile().mkdirs();

            String thisLine;
            StringBuffer scriptCnt = new StringBuffer();
            BufferedReader r = new BufferedReader(new FileReader(pfile));
            while ((thisLine = r.readLine()) != null) scriptCnt.append(thisLine).append("\n");
            r.close();

            ProcessModel oprocess;
            try {
                FileOutputStream cbpFos = new FileOutputStream(targetCbp, false);
                Descriptor desc = new Descriptor();
                oprocess = _compiler.compileProcess(scriptCnt.toString(), desc);
                Serializer ser = new Serializer();
                ser.writePModel(oprocess, cbpFos);
                cbpFos.close();

                _processes.put(oprocess.getQName(), oprocess);
                _descriptors.put(oprocess.getQName(), desc);

                fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.DEPLOYED, oprocess.getQName(), null));
                fireEvent(new ProcessStoreEvent(ProcessStoreEvent.Type.ACTIVATED, oprocess.getQName(), null));
            } catch (CompilationException e) {
                throw new RuntimeException("There were errors during the compilation of a SimPEL process:\n" + e.toString());
            }

            return oprocess;
        }

        private ArrayList<File> listFilesRecursively(File root, FileFilter filter) {
            ArrayList<File> result = new ArrayList<File>();
            // Filtering the files we're interested in in the current directory
            File[] select = root.listFiles(filter);
            for (File file : select) {
                result.add(file);
            }
            // Then we can check the directories
            File[] all = root.listFiles();
            for (File file : all) {
                if (file.isDirectory())
                    result.addAll(listFilesRecursively(file, filter));
            }
            return result;
        }

        /**
         * Path of a file relative to a directory. The file has to be (indirectly) contained
         * in that directory.
         */
        private String relativePath(File toDir, File fromFile) {
            if (!fromFile.equals(toDir)) {
                File parent = fromFile.getParentFile();
                return relativePath(toDir, parent) + "/" + fromFile.getName();
            } else return "";
        }

        private String noExt(String f) {
            return f.substring(0, f.lastIndexOf("."));
        }
    }

}

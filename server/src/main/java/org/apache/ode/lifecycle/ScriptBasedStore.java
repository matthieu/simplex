package org.apache.ode.lifecycle;

import org.apache.ode.embed.EmbeddedStore;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class ScriptBasedStore extends EmbeddedStore {

    private File _scriptsDir;
    private File _workDir;

    public ScriptBasedStore(File _scriptsDir) {
        this._scriptsDir = _scriptsDir;
        new ScriptPoller();
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
            List<File> scripts = listFilesRecursively(_scriptsDir, _scriptsFilter);
            List<File> cbps = listFilesRecursively(_workDir, _cbpFilter);
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

    }

}

package org.apache.ode.simpel.util;

import org.apache.ode.simpel.ErrorListener;
import org.apache.ode.simpel.CompilationException;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class DefaultErrorListener implements ErrorListener {

    private static final Logger __log = Logger.getLogger(DefaultErrorListener.class);

    private LinkedList<CompilationException.Error> _errors = new LinkedList<CompilationException.Error>();

    public List<CompilationException.Error> getErrors() {
        return _errors;
    }

    public void reportRecognitionError(int line, int column, String message, Exception e) {
        _errors.add(new CompilationException.Error(line, column, message, e));
        __log.debug(line + ":" + column + " " +  message);
    }

}

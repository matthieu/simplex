package org.apache.ode.simpel.util;

import org.antlr.runtime.RecognitionException;
import org.apache.ode.simpel.ErrorListener;
import org.apache.ode.simpel.CompilationException;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class DefaultErrorListener implements ErrorListener {

    private LinkedList<CompilationException.Error> _errors = new LinkedList<CompilationException.Error>();

    public List<CompilationException.Error> getErrors() {
        return _errors;
    }

    public void reportRecognitionError(int line, int column, String message, Exception e) {
        _errors.add(new CompilationException.Error(line, column, message, e));
        System.err.println(line + ":" + column + " " +  message);
    }

}

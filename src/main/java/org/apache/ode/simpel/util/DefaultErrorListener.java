package org.apache.ode.simpel.util;

import org.antlr.runtime.RecognitionException;
import org.apache.ode.simpel.ErrorListener;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class DefaultErrorListener implements ErrorListener {

    private LinkedList<Error> _errors = new LinkedList<Error>();

    public List<Error> getErrors() {
        return _errors;
    }

    public void reportRecognitionError(int line, int column, String message, RecognitionException e) {
        _errors.add(new Error(line, column, message, e));
        System.err.println(line + ":" + column + " " +  message);
    }

    public class Error {
        public int line;
        public int column;
        public String message;
        public RecognitionException e;

        public Error(int line, int column, String message, RecognitionException e) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.e = e;
        }
    }
}

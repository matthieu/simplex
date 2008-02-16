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

    public void reportRecognitionError(String[] tokens, int line, String message, RecognitionException e) {
        _errors.add(new Error(tokens, line, message, e));
        System.err.println("line " + line + ": " +  message);
    }

    public class Error {
        public String tokens[];
        public int line;
        public String message;
        public RecognitionException e;

        public Error(String[] tokens, int line, String message, RecognitionException e) {
            this.tokens = tokens;
            this.line = line;
            this.message = message;
            this.e = e;
        }
    }
}

package org.apache.ode.simpel.util;

import org.antlr.runtime.RecognitionException;
import org.apache.ode.simpel.ErrorListener;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class DefaultErrorListener implements ErrorListener {

    public void reportRecognitionError(String[] tokens, int line, String message, RecognitionException e) {
        System.err.println("line " + line + ": " +  message);
    }
}

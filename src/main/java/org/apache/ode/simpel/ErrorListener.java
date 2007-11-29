package org.apache.ode.simpel;

import org.antlr.runtime.RecognitionException;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public interface ErrorListener {

    public void reportRecognitionError(String[] tokens, int line, String message, RecognitionException e);
}

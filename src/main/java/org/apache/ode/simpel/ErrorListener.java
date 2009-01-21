package org.apache.ode.simpel;

import org.antlr.runtime.RecognitionException;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public interface ErrorListener {

    public void reportRecognitionError(int line, int column, String message, RecognitionException e);
}

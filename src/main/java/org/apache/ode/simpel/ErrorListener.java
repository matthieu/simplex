package org.apache.ode.simpel;

import org.antlr.runtime.RecognitionException;

import java.util.List;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public interface ErrorListener {

    List<CompilationException.Error> getErrors();

    void reportRecognitionError(int line, int column, String message, RecognitionException e);
}

package org.apache.ode.simpel;

/**
 * @author Matthieu Riou <mriou@apache.org>
 */
public class CompilationException extends RuntimeException {
    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompilationException(Throwable cause) {
        super(cause);
    }
}

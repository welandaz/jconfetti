package io.github.welandaz;

/**
 * Thrown when {@link Confetti#map(ConfigurationUnit, Class)} or related mapping methods
 * cannot bind configuration data to the requested Java type.
 */
public final class ConfettiMappingException extends RuntimeException {

    ConfettiMappingException(final String message) {
        super(message);
    }

    ConfettiMappingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}

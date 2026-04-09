package io.github.welandaz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the directive name that maps to an annotated field.
 *
 * <pre>
 * class Server {
 *     &#64;ConfettiName("listen_port")
 *     int port;
 * }
 * </pre>
 *
 * Without this annotation, the field name itself is used as the directive name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfettiName {

    String value();

}

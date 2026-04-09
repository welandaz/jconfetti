package io.github.welandaz;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single Confetti directive.
 * Each directive has one or more arguments and zero or more subdirectives.
 */
public final class Directive {

    private final List<String> arguments;
    private final List<Directive> subdirectives;

    Directive(final List<String> arguments, final List<Directive> subdirectives) {
        this.arguments = Collections.unmodifiableList(arguments);
        this.subdirectives = Collections.unmodifiableList(subdirectives);
    }

    /**
     * Returns the directive arguments in source order.
     * The first element is the directive name.
     */
    public List<String> arguments() {
        return arguments;
    }

    /**
     * Returns the nested subdirectives in source order.
     */
    public List<Directive> subdirectives() {
        return subdirectives;
    }

    /**
     * Returns the directive name, which is the first element of {@link #arguments()}.
     */
    public String name() {
        return arguments.get(0);
    }

    /**
     * Returns the value arguments after the directive name in source order.
     */
    public List<String> values() {
        return Collections.unmodifiableList(arguments.subList(1, arguments.size()));
    }

    /**
     * Returns the number of value arguments after the directive name.
     */
    public int valueCount() {
        return arguments.size() - 1;
    }

    /**
     * Returns whether this directive contains one or more value arguments after the directive name.
     */
    public boolean hasValues() {
        return valueCount() > 0;
    }

    /**
     * Returns the value argument at the given zero-based index.
     *
     * @param index zero-based index within the directive value arguments
     * @return the value argument at the given index
     * @throws IndexOutOfBoundsException if the index is negative or greater than or equal to {@link #valueCount()}
     */
    public String value(final int index) {
        return arguments.get(index + 1);
    }

    /**
     * Returns whether this directive contains nested subdirectives.
     */
    public boolean hasSubdirectives() {
        return !subdirectives.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Directive)) {
            return false;
        }
        Directive directive = (Directive) object;
        return arguments.equals(directive.arguments)
                && subdirectives.equals(directive.subdirectives);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arguments, subdirectives);
    }

    @Override
    public String toString() {
        return "Directive{"
                + "arguments=" + arguments
                + ", subdirectives=" + subdirectives
                + '}';
    }

}

package io.github.welandaz;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The root of a parsed Confetti document.
 */
public final class ConfigurationUnit {

    private final List<Directive> directives;

    ConfigurationUnit(final List<Directive> directives) {
        this.directives = Collections.unmodifiableList(directives);
    }

    /**
     * Returns the top-level directives in document order.
     */
    public List<Directive> directives() {
        return directives;
    }

    /**
     * Returns the first top-level directive with the given name, or {@code null} if not found.
     */
    public Directive directive(final String name) {
        Objects.requireNonNull(name, "name");

        return directives.stream()
                .filter(directive -> directive.arguments().get(0).equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all top-level directives with the given name.
     */
    public List<Directive> directives(final String name) {
        Objects.requireNonNull(name, "name");

        return Collections.unmodifiableList(directives.stream()
                .filter(directive -> directive.arguments().get(0).equals(name))
                .collect(Collectors.toList()));
    }

    /**
     * Returns whether a top-level directive with the given name exists.
     */
    public boolean hasDirective(final String name) {
        return directive(name) != null;
    }

    /**
     * Returns whether the document contains no directives.
     */
    public boolean isEmpty() {
        return directives.isEmpty();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ConfigurationUnit)) {
            return false;
        }
        ConfigurationUnit that = (ConfigurationUnit) object;
        return directives.equals(that.directives);
    }

    @Override
    public int hashCode() {
        return directives.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigurationUnit{"
                + "directives=" + directives
                + '}';
    }

}

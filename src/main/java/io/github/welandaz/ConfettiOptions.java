package io.github.welandaz;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parser options for Confetti's optional extensions.
 */
public final class ConfettiOptions {

    private static final ConfettiOptions DEFAULTS = new ConfettiOptions(false, false, Collections.emptySet());

    private final boolean cStyleComments;
    private final boolean expressionArguments;
    private final Set<String> punctuators;

    private ConfettiOptions(
            final boolean cStyleComments,
            final boolean expressionArguments,
            final Set<String> punctuators
    ) {
        this.cStyleComments = cStyleComments;
        this.expressionArguments = expressionArguments;
        this.punctuators = punctuators;
    }

    /**
     * Returns the default parser options with all optional extensions disabled.
     */
    public static ConfettiOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns whether C-style line and block comments are enabled.
     */
    public boolean isCStyleComments() {
        return cStyleComments;
    }

    /**
     * Returns whether parenthesized expression arguments are enabled.
     */
    public boolean isExpressionArguments() {
        return expressionArguments;
    }

    /**
     * Returns the configured punctuator arguments.
     */
    public Set<String> punctuators() {
        return punctuators;
    }

    /**
     * Returns a builder for custom parser options.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfettiOptions)) {
            return false;
        }
        ConfettiOptions that = (ConfettiOptions) o;
        return cStyleComments == that.cStyleComments
                && expressionArguments == that.expressionArguments
                && punctuators.equals(that.punctuators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cStyleComments, expressionArguments, punctuators);
    }

    @Override
    public String toString() {
        return "ConfettiOptions{"
                + "cStyleComments=" + cStyleComments
                + ", expressionArguments=" + expressionArguments
                + ", punctuators=" + punctuators
                + '}';
    }

    /**
     * Builder for {@link ConfettiOptions}.
     */
    public static final class Builder {

        private boolean cStyleComments;
        private boolean expressionArguments;
        private Set<String> punctuators = Collections.emptySet();

        private Builder() {
        }

        public Builder cStyleComments(final boolean enabled) {
            this.cStyleComments = enabled;
            return this;
        }

        public Builder expressionArguments(final boolean enabled) {
            this.expressionArguments = enabled;
            return this;
        }

        /**
         * Enables punctuator arguments for the given punctuator strings.
         */
        public Builder punctuators(final String... punctuators) {
            this.punctuators = copyPunctuators(punctuators);
            return this;
        }

        public ConfettiOptions build() {
            return new ConfettiOptions(cStyleComments, expressionArguments, punctuators);
        }

    }

    private static Set<String> copyPunctuators(final String... punctuators) {
        if (punctuators == null || punctuators.length == 0) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(
                Arrays.stream(punctuators)
                        .peek(punctuator -> {
                            if (punctuator == null || punctuator.isEmpty()) {
                                throw new IllegalArgumentException("Punctuator cannot be null or empty");
                            }
                        })
                        .collect(Collectors.toSet())
        );
    }

}

package io.github.welandaz;

/**
 * Thrown when a Confetti configuration cannot be parsed.
 * Carries the character position and line where the error was detected.
 */
public final class ConfettiParseException extends RuntimeException {

    private final int position;
    private final int line;

    ConfettiParseException(final String message, final int position, final int line) {
        super(formatMessage(message, line));
        this.position = position;
        this.line = line;
    }

    ConfettiParseException(final String message, final Throwable cause) {
        super(message, cause);
        this.position = -1;
        this.line = -1;
    }

    /**
     * Character position in the input where the error was detected, or -1 if unknown.
     */
    public int position() {
        return position;
    }

    /**
     * line number where the error was detected, or -1 if unknown.
     */
    public int line() {
        return line;
    }

    private static String formatMessage(final String message, final int line) {
        return message + " (line " + line + ")";
    }

    /**
     * Computes line and column for a given position in the input, accounting for
     * all Unicode line terminators recognized by Confetti.
     */
    static ConfettiParseException at(final String input, final int position, final String message) {
        int line = 1;
        for (int i = 0; i < position && i < input.length(); i++) {
            if (Characters.isLineTerminator(input.charAt(i))) {
                line++;
                i = Characters.advancePastLineTerminator(input, i, input.length()) - 1;
            }
        }

        return new ConfettiParseException(message, position, line);
    }

}

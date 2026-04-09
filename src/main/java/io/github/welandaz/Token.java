package io.github.welandaz;

final class Token {

    private Type type;
    private int start;
    private String value;

    void set(final Type type, final int start) {
        set(type, start, null);
    }

    void set(final Type type, final int start, final String value) {
        this.type = type;
        this.start = start;
        this.value = value;
    }

    Type type() {
        return type;
    }

    int start() {
        return start;
    }

    String value() {
        return value;
    }

    boolean isNotArgumentToken() {
        return type != Token.Type.ARGUMENT
                && type != Token.Type.STRING
                && type != Token.Type.EXPRESSION
                && type != Token.Type.PUNCTUATION;
    }

    enum Type {
        ARGUMENT,
        STRING,
        EXPRESSION,
        PUNCTUATION,
        LEFT_BRACE,
        RIGHT_BRACE,
        SEMICOLON,
        NEWLINE,
        LINE_CONTINUATION,
        END_OF_FILE
    }

}

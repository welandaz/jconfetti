package io.github.welandaz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Parser {

    private final Tokenizer tokenizer;

    private Token currentToken;

    Parser(final String input, final ConfettiOptions confettiOptions) {
        this.tokenizer = new Tokenizer(input, confettiOptions);
    }

    ConfigurationUnit parse() {
        currentToken = tokenizer.next();

        return new ConfigurationUnit(parseDirectives(false));
    }

    private List<Directive> parseDirectives(boolean inBlock) {
        final List<Directive> directives = new ArrayList<>();
        while (true) {
            skipNewlines();

            if (currentToken.type() == Token.Type.END_OF_FILE) {
                if (inBlock) {
                    throw tokenizer.errorAt(currentToken.start(), "Missing closing brace '}'");
                }
                return directives;
            }

            if (currentToken.type() == Token.Type.RIGHT_BRACE) {
                if (!inBlock) {
                    throw tokenizer.errorAt(currentToken.start(), "Unexpected right brace");
                }
                return directives;
            }

            directives.add(parseDirective());
        }
    }

    private Directive parseDirective() {
        final List<String> arguments = parseArguments();
        final boolean sawNewline = consumeDirectiveSpacingAfterArguments();

        if (currentToken.type() == Token.Type.LEFT_BRACE) {
            return new Directive(arguments, parseSubdirectiveBlock());
        }

        return new Directive(arguments, parseSimpleDirectiveTail(sawNewline));
    }

    private List<String> parseArguments() {
        final List<String> arguments = new ArrayList<>();
        while (true) {
            while (currentToken.type() == Token.Type.LINE_CONTINUATION) {
                if (arguments.isEmpty()) {
                    throw tokenizer.errorAt(currentToken.start(), "Unexpected line continuation");
                }
                currentToken = tokenizer.next();
            }

            if (currentToken.isNotArgumentToken()) {
                break;
            }

            arguments.add(consumeArgument());
        }

        if (arguments.isEmpty()) {
            throw tokenizer.errorAt(currentToken.start(), "Expected arguments");
        }

        return arguments;
    }

    private List<Directive> parseSimpleDirectiveTail(final boolean sawNewline) {
        if (currentToken.type() == Token.Type.SEMICOLON) {
            if (sawNewline) {
                throw tokenizer.errorAt(currentToken.start(), "Unexpected semicolon on new line");
            }

            currentToken = tokenizer.next();
            return Collections.emptyList();
        }

        if (currentToken.type() == Token.Type.RIGHT_BRACE || currentToken.type() == Token.Type.END_OF_FILE || sawNewline) {
            return Collections.emptyList();
        }

        throw tokenizer.errorAt(currentToken.start(), "Expected terminator");
    }

    private boolean consumeDirectiveSpacingAfterArguments() {
        boolean sawNewline = false;
        while (currentToken.type() == Token.Type.NEWLINE || currentToken.type() == Token.Type.LINE_CONTINUATION) {
            if (currentToken.type() == Token.Type.NEWLINE) {
                sawNewline = true;
            }
            currentToken = tokenizer.next();
        }

        return sawNewline;
    }

    private List<Directive> parseSubdirectiveBlock() {
        consume(Token.Type.LEFT_BRACE);
        final List<Directive> subdirectives = parseDirectives(true);
        consume(Token.Type.RIGHT_BRACE);

        if (currentToken.type() == Token.Type.SEMICOLON) {
            currentToken = tokenizer.next();
        }

        return subdirectives;
    }

    private String consumeArgument() {
        final String value = currentToken.value();
        currentToken = tokenizer.next();

        return value;
    }

    private void skipNewlines() {
        while (currentToken.type() == Token.Type.NEWLINE) {
            currentToken = tokenizer.next();
        }
    }

    private void consume(Token.Type type) {
        if (currentToken.type() != type) {
            throw tokenizer.errorAt(currentToken.start(), "Expected " + type);
        }

        currentToken = tokenizer.next();
    }

}

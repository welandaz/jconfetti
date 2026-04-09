package io.github.welandaz;

import java.util.*;

final class Tokenizer {

    private final String input;
    private final ConfettiOptions confettiOptions;
    private final Map<Character, List<String>> punctuatorsByStarter;
    private final Token token;

    private int position = 0;

    Tokenizer(String input, ConfettiOptions confettiOptions) {
        this.input = input;
        this.confettiOptions = confettiOptions;
        this.token = new Token();
        this.punctuatorsByStarter = buildPunctuatorMatcher(confettiOptions.punctuators());
    }

    Token next() {
        while (true) {
            skipWhitespace();

            if (position >= input.length()) {
                token.set(Token.Type.END_OF_FILE, position);

                return token;
            }

            final char character = input.charAt(position);
            if (character == '#') {
                skipLineComment();
                continue;
            }

            if (confettiOptions.isCStyleComments() && character == '/') {
                if (beginsWith("//")) {
                    skipLineComment();
                    continue;
                }

                if (beginsWith("/*")) {
                    skipBlockComment();
                    continue;
                }
            }

            switch (character) {
                case '{':
                    token.set(Token.Type.LEFT_BRACE, position);
                    position++;
                    return token;
                case '}':
                    token.set(Token.Type.RIGHT_BRACE, position);
                    position++;
                    return token;
                case ';':
                    token.set(Token.Type.SEMICOLON, position);
                    position++;
                    return token;
                case '\n':
                case '\f':
                case '\u000B':
                case '\u0085':
                case '\u2028':
                case '\u2029':
                    token.set(Token.Type.NEWLINE, position);
                    position++;
                    return token;
                case '\r': {
                    int start = position;
                    position = Characters.advancePastLineTerminator(input, position, input.length());
                    token.set(Token.Type.NEWLINE, start);
                    return token;
                }
                case '\\':
                    if (position + 1 < input.length() && Characters.isLineTerminator(input.charAt(position + 1))) {
                        int start = position;
                        position = Characters.advancePastLineTerminator(input, position + 1, input.length());
                        token.set(Token.Type.LINE_CONTINUATION, start);
                        return token;
                    }
                    break;
                case '"':
                    if (beginsWith("\"\"\"")) {
                        return tripleQuotedString();
                    }
                    return quotedString();
                case '(':
                    if (confettiOptions.isExpressionArguments()) {
                        return expression();
                    }
                    break;
                default:
                    break;
            }

            final String longestPunctuator = findLongestPunctuatorAtPosition();
            if (longestPunctuator != null) {
                final int start = position;
                position += longestPunctuator.length();
                token.set(Token.Type.PUNCTUATION, start, longestPunctuator);
                return token;
            }

            return unquotedArgument();
        }
    }

    private static Map<Character, List<String>> buildPunctuatorMatcher(final Set<String> punctuators) {
        if (punctuators.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Character, List<String>> byStarter = new HashMap<>();
        for (final String punctuator : punctuators) {
            final char starter = punctuator.charAt(0);
            final List<String> candidates = byStarter.computeIfAbsent(starter, it -> new ArrayList<>());

            candidates.add(punctuator);
        }

        byStarter.values().forEach(candidates -> candidates.sort((left, right) -> {
            int compareResult = Integer.compare(right.length(), left.length());
            if (compareResult != 0) {
                return compareResult;
            }

            return left.compareTo(right);
        }));

        return byStarter;
    }

    private String findLongestPunctuatorAtPosition() {
        if (position >= input.length()) {
            return null;
        }

        final List<String> candidates = punctuatorsByStarter.get(input.charAt(position));
        if (candidates == null) {
            return null;
        }

        return candidates.stream()
                .filter(candidate -> input.startsWith(candidate, position))
                .findFirst()
                .orElse(null);

    }

    private Token tripleQuotedString() {
        int start = position;
        position += 3;

        final StringBuilder stringBuilder = new StringBuilder();
        while (position < input.length()) {
            if (beginsWith("\"\"\"")) {
                position += 3;
                token.set(Token.Type.STRING, start, stringBuilder.toString());
                return token;
            }

            final char character = input.charAt(position);
            if (character == '\\') {
                final int escapedIndex = position + 1;
                if (escapedIndex >= input.length()) {
                    throw errorAt(escapedIndex, "Incomplete escape sequence");
                }
                final char next = input.charAt(escapedIndex);
                if (Characters.isLineTerminator(next)) {
                    throw errorAt(escapedIndex, "Incomplete escape sequence");
                }
                if (Characters.isSpaceButNotLineTerminator(next)) {
                    throw errorAt(escapedIndex, "Illegal escape character");
                }
                stringBuilder.append(next);
                position = escapedIndex + 1;
                continue;
            }

            stringBuilder.append(character);
            position++;
        }

        throw errorAt(position, "Unclosed multiline string");
    }

    private Token expression() {
        final int start = position;
        final int valueStart = ++position;
        int depth = 1;
        while (position < input.length() && depth > 0) {
            char character = input.charAt(position);
            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;
            }
            position++;
        }

        if (depth != 0) {
            throw errorAt(position, "Unbalanced parentheses");
        }

        token.set(Token.Type.EXPRESSION, start, input.substring(valueStart, position - 1));
        return token;
    }

    private Token unquotedArgument() {
        final int start = position;
        final StringBuilder stringBuilder = new StringBuilder();
        while (position < input.length()) {
            final char character = input.charAt(position);

            if (character == '{' || character == '}' || character == ';' || character == '"') {
                break;
            }

            if (Characters.isLineTerminator(character)) {
                break;
            }

            if (Characters.isSpaceButNotLineTerminator(character)) {
                break;
            }

            if (character == '(' && confettiOptions.isExpressionArguments()) {
                break;
            }

            if (findLongestPunctuatorAtPosition() != null) {
                break;
            }

            if (character == '\\') {
                if (position + 1 >= input.length()) {
                    throw errorAt(position, "Incomplete escape sequence");
                }

                final char next = input.charAt(position + 1);
                if (Characters.isLineTerminator(next)) {
                    throw errorAt(position, "Illegal escape character");
                }

                if (Characters.isSpaceButNotLineTerminator(next)) {
                    throw errorAt(position, "Illegal escape character");

                }
                stringBuilder.append(next);
                position += 2;
                continue;
            }

            stringBuilder.append(character);
            position++;
        }

        token.set(Token.Type.ARGUMENT, start, stringBuilder.toString());
        return token;
    }

    private Token quotedString() {
        final int start = position;
        position++;
        final StringBuilder stringBuilder = new StringBuilder();
        while (position < input.length()) {
            char character = input.charAt(position++);

            if (character == '\\') {
                if (position >= input.length()) {
                    throw errorAt(position, "Incomplete escape sequence");
                }

                final char next = input.charAt(position);
                if (Characters.isLineTerminator(next)) {
                    position = Characters.advancePastLineTerminator(input, position, input.length());
                    continue;
                }

                if (Characters.isSpaceButNotLineTerminator(next)) {
                    throw errorAt(position, "Illegal escape character");
                }

                stringBuilder.append(next);
                position++;
                continue;
            }

            if (Characters.isLineTerminator(character)) {
                throw errorAt(position, "Unclosed quote");
            }

            if (character == '"') {
                token.set(Token.Type.STRING, start, stringBuilder.toString());
                return token;
            }

            stringBuilder.append(character);
        }

        throw errorAt(position, "Unclosed quote");
    }

    private void skipWhitespace() {
        while (position < input.length()) {
            final char character = input.charAt(position);
            if (Characters.isLineTerminator(character)) {
                break;
            }

            if (Characters.isSpaceButNotLineTerminator(character)) {
                position++;
                continue;
            }

            break;
        }
    }

    private void skipLineComment() {
        while (position < input.length() && Characters.isNotLineTerminator(input.charAt(position))) {
            position++;
        }
    }

    private void skipBlockComment() {
        position += 2;
        while (position < input.length() && !beginsWith("*/")) {
            position++;
        }

        if (position >= input.length()) {
            throw errorAt(position, "Unterminated block comment");
        }

        position += 2;
    }

    private boolean beginsWith(final String prefix) {
        return input.startsWith(prefix, position);
    }

    ConfettiParseException errorAt(final int position, final String message) {
        return ConfettiParseException.at(input, position, message);
    }

}

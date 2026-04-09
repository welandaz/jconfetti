package io.github.welandaz;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Parser for Confetti configuration documents.
 *
 * <p>Create an instance with default options via {@link #Confetti()}, or provide explicit defaults for repeated
 * parsing via {@link #Confetti(ConfettiOptions)}. For one-off overrides, use the {@code parse(..., ConfettiOptions)}
 * or {@code map(..., ConfettiOptions)} overloads.</p>
 */
public final class Confetti {

    private final ConfettiOptions options;

    /**
     * Creates a parser with {@linkplain ConfettiOptions#defaults() default options}.
     */
    public Confetti() {
        this(ConfettiOptions.defaults());
    }

    /**
     * Creates a parser with the provided default options.
     *
     * @param options parser options to use for {@link #parse(String)} and {@link #parse(Path)}
     */
    public Confetti(final ConfettiOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Returns this parser's default options.
     */
    public ConfettiOptions options() {
        return options;
    }

    /**
     * Parses a Confetti string using this parser's default options.
     */
    public ConfigurationUnit parse(final String input) {
        return parse(input, options);
    }

    /**
     * Parses a UTF-8 Confetti file using this parser's default options.
     */
    public ConfigurationUnit parse(final Path path) throws IOException {
        return parse(path, options);
    }

    /**
     * Parses a UTF-8 Confetti file using the provided options for this call.
     */
    public ConfigurationUnit parse(final Path path, final ConfettiOptions confettiOptions) throws IOException {
        return parse(
                decodeUtf8(Files.readAllBytes(Objects.requireNonNull(path, "path"))),
                Objects.requireNonNull(confettiOptions, "options")
        );
    }

    /**
     * Parses a Confetti string using the provided options for this call.
     */
    public ConfigurationUnit parse(final String input, final ConfettiOptions confettiOptions) {
        return new Parser(
                prepareInput(Objects.requireNonNull(input, "input")),
                Objects.requireNonNull(confettiOptions, "options")
        ).parse();
    }

    /**
     * Parses a Confetti string using this parser's default options and maps the result to the given class.
     */
    public <T> T map(final String input, final Class<T> type) {
        return map(parse(input), type);
    }

    /**
     * Parses a UTF-8 Confetti file using this parser's default options and maps the result to the given class.
     */
    public <T> T map(final Path path, final Class<T> type) throws IOException {
        return map(parse(path), type);
    }

    /**
     * Parses a Confetti string using the provided options for this call and maps the result to the given class.
     */
    public <T> T map(final String input, final Class<T> type, final ConfettiOptions confettiOptions) {
        return map(parse(input, confettiOptions), type);
    }

    /**
     * Parses a UTF-8 Confetti file using the provided options for this call and maps the result to the given class.
     */
    public <T> T map(final Path path, final Class<T> type, final ConfettiOptions confettiOptions) throws IOException {
        return map(parse(path, confettiOptions), type);
    }

    /**
     * Maps a parsed configuration unit onto a new instance of the given class.
     */
    public <T> T map(final ConfigurationUnit unit, final Class<T> type) {
        return ConfettiMapper.map(Objects.requireNonNull(unit, "unit"), Objects.requireNonNull(type, "type"));
    }

    private static String decodeUtf8(final byte[] input) {
        final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return charsetDecoder.decode(ByteBuffer.wrap(input)).toString();
        } catch (CharacterCodingException e) {
            throw new ConfettiParseException("Invalid UTF-8 input", e);
        }
    }

    private static String prepareInput(String input) {
        if (input.startsWith("\uFEFF")) {
            input = input.substring(1);
        }

        final int substituteCharacter = input.indexOf('\u001A');
        if (substituteCharacter >= 0) {
            if (substituteCharacter < input.length() - 1) {
                throw ConfettiParseException.at(input, substituteCharacter, String.format("Invalid character U+001A at position %d", substituteCharacter));
            }
            input = input.substring(0, substituteCharacter);
        }

        for (int i = 0; i < input.length();) {
            int codePointStart = i;
            int codePoint;
            if (
                    i + 1 < input.length()
                            && Character.isHighSurrogate(input.charAt(i))
                            && Character.isLowSurrogate(input.charAt(i + 1))
            ) {
                codePoint = Character.toCodePoint(input.charAt(i), input.charAt(i + 1));
                i += 2;
            } else {
                codePoint = input.charAt(i);
                i++;
            }

            if (Characters.isForbiddenCodePoint(codePoint)) {
                throw ConfettiParseException.at(
                        input,
                        codePointStart,
                        String.format("Invalid character U+%04X at position %d", codePoint, codePointStart)
                );
            }
        }

        return input;
    }

}

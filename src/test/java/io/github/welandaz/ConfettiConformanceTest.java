package io.github.welandaz;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

final class ConfettiConformanceTest {

    private static final Path TEST_DIRECTORY = Paths.get("src/test/resources/conformance");
    private static final Confetti CONFETTI = new Confetti();

    @TestFactory
    Stream<DynamicTest> confettiTests() throws IOException {
        final Map<String, Map<String, Path>> grouped;
        try (Stream<Path> paths = Files.list(TEST_DIRECTORY)) {
            grouped = paths.collect(Collectors.groupingBy(
                    ConfettiConformanceTest::baseName,
                    Collectors.toMap(
                            ConfettiConformanceTest::extension,
                            path -> path
                    )
            ));
        }

        return grouped.entrySet().stream()
                .map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> runTest(entry.getValue())));
    }

    private void runTest(final Map<String, Path> files) throws IOException {
        final ConfettiOptions.Builder builder = ConfettiOptions.builder()
                .cStyleComments(files.containsKey("ext_c_style_comments"))
                .expressionArguments(files.containsKey("ext_expression_arguments"));

        if (files.containsKey("ext_punctuator_arguments")) {
            final String content = new String(
                    Files.readAllBytes(files.get("ext_punctuator_arguments")),
                    StandardCharsets.UTF_8
            );
            final List<String> punctuators = new ArrayList<>();
            for (String line : content.split("\r?\n|\r|\f|\u000B|\u0085|\u2028|\u2029")) {
                if (!line.isEmpty()) {
                    punctuators.add(line);
                }
            }
            builder.punctuators(punctuators.toArray(new String[0]));
        }

        final ConfettiOptions confettiOptions = builder.build();
        if (files.containsKey("pass")) {
            final String expected = new String(Files.readAllBytes(files.get("pass")), StandardCharsets.UTF_8).trim();
            final ConfigurationUnit configurationUnit = CONFETTI.parse(files.get("conf"), confettiOptions);

            assertEquals(expected, serialize(configurationUnit));
            return;
        }

        if (files.containsKey("fail")) {
            assertThrows(ConfettiParseException.class, () -> CONFETTI.parse(files.get("conf"), confettiOptions));
            return;
        }

        fail("Test must have either .pass or .fail file");
    }

    private static String baseName(final Path path) {
        final String file = path.getFileName().toString();

        return file.substring(0, file.lastIndexOf('.'));
    }

    private static String extension(final Path path) {
        final String file = path.getFileName().toString();

        return file.substring(file.lastIndexOf('.') + 1);
    }

    private String serialize(final ConfigurationUnit configurationUnit) {
        final StringBuilder stringBuilder = new StringBuilder();
        configurationUnit.directives().forEach(directive -> serializeDirective(directive, stringBuilder, 0));

        return stringBuilder.toString().trim();
    }

    private void serializeDirective(final Directive directive, final StringBuilder stringBuilder, final int indent) {
        indent(stringBuilder, indent);

        for (int i = 0; i < directive.arguments().size(); i++) {
            if (i == 0) {
                stringBuilder.append("<");
            } else {
                stringBuilder.append(" <");
            }
            stringBuilder.append(directive.arguments().get(i)).append(">");
        }

        if (!directive.subdirectives().isEmpty()) {
            stringBuilder.append(" [\n");

            for (final Directive child : directive.subdirectives()) {
                serializeDirective(child, stringBuilder, indent + 4);
            }

            indent(stringBuilder, indent);
            stringBuilder.append("]");
        }

        stringBuilder.append("\n");
    }

    private static void indent(final StringBuilder stringBuilder, final int spaces) {
        for (int i = 0; i < spaces; i++) {
            stringBuilder.append(' ');
        }
    }

}

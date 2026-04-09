package io.github.welandaz;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfettiPublicApiTest {

    private static final Confetti CONFETTI = new Confetti();

    @Test
    void directiveExposesSpecShapedStructure() {
        final ConfigurationUnit configurationUnit = CONFETTI.parse(
                "server localhost 8080 {\n" +
                "    port 80\n" +
                "}\n" +
                "server example.com 443\n"
        );

        assertTrue(configurationUnit.hasDirective("server"));
        assertFalse(configurationUnit.hasDirective("database"));
        assertEquals(2, configurationUnit.directives("server").size());

        final Directive firstServer = configurationUnit.directive("server");
        assertNotNull(firstServer);
        assertEquals(Arrays.asList("server", "localhost", "8080"), firstServer.arguments());
        assertTrue(firstServer.hasValues());
        assertEquals(Arrays.asList("localhost", "8080"), firstServer.values());
        assertEquals(1, firstServer.subdirectives().size());
        assertEquals(Arrays.asList("port", "80"), firstServer.subdirectives().get(0).arguments());
        assertTrue(firstServer.subdirectives().get(0).hasValues());
        assertEquals(Collections.singletonList("80"), firstServer.subdirectives().get(0).values());
        assertEquals(Collections.emptyList(), firstServer.subdirectives().get(0).subdirectives());
    }

    @Test
    void returnableCollectionsAreImmutable() {
        final ConfigurationUnit configurationUnit = CONFETTI.parse("server localhost { port 80 }");
        final Directive directive = configurationUnit.directive("server");

        assertThrows(UnsupportedOperationException.class, () -> configurationUnit.directives().add(directive));
        assertThrows(UnsupportedOperationException.class, () -> directive.arguments().add("443"));
        assertThrows(UnsupportedOperationException.class, () -> directive.values().add("443"));
        assertThrows(UnsupportedOperationException.class, () -> directive.subdirectives().clear());
    }

    @Test
    void confettiOptionsApiValidation() {
        final ConfettiOptions confettiOptions = ConfettiOptions.builder()
                .cStyleComments(true)
                .expressionArguments(true)
                .punctuators("=", ":=")
                .build();

        assertTrue(confettiOptions.isCStyleComments());
        assertTrue(confettiOptions.isExpressionArguments());
        assertEquals(2, confettiOptions.punctuators().size());
        assertThrows(UnsupportedOperationException.class, () -> confettiOptions.punctuators().add("!="));
    }

    @Test
    void confettiInstanceCarriesOptions() {
        final ConfettiOptions confettiOptions = ConfettiOptions.builder()
                .cStyleComments(true)
                .build();
        final Confetti parser = new Confetti(confettiOptions);

        final ConfigurationUnit configurationUnit = parser.parse("// comment\nname value\n");

        assertSame(confettiOptions, parser.options());
        assertEquals(Arrays.asList("name", "value"), configurationUnit.directive("name").arguments());
    }

}

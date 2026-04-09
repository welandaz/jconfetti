package io.github.welandaz;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ConfettiMappingTest {

    private static final Confetti CONFETTI = new Confetti();

    static final class ServerConfig {
        ServerSection server;
    }

    static final class ServerSection {
        String host;
        int port;
        boolean debug;
    }

    static final class ApplicationConfig {
        String title;
        DatabaseSection database;
        List<String> tags;
    }

    static final class DatabaseSection {
        String url;
        @ConfettiName("pool_size")
        int poolSize;
    }

    static final class UsersConfig {
        List<UserSection> user;
    }

    static final class UserSection {
        String name;
        String role;
    }

    static final class EnvironmentConfig {
        Map<String, String> env;
    }

    static final class ConcreteCollectionsConfig {
        ArrayList<String> tags;
        LinkedHashMap<String, String> env;
    }

    static final class TitleOnlyConfig {
        String title;
    }

    static final class TagsOnlyConfig {
        List<String> tags;
    }

    static final class NestedDefaultsConfig {
        DefaultServerConfig server = new DefaultServerConfig();
    }

    static final class DefaultServerConfig {
        String host = "localhost";
        int port = 80;
        boolean debug = true;
    }

    @Test
    void nestedObject() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "server {\n" +
                "    host 127.0.0.1\n" +
                "    port 8080\n" +
                "    debug true\n" +
                "}"
        );

        final ServerConfig config = CONFETTI.map(unit, ServerConfig.class);

        assertNotNull(config.server);
        assertEquals("127.0.0.1", config.server.host);
        assertEquals(8080, config.server.port);
        assertTrue(config.server.debug);
    }

    @Test
    void annotatedFieldName() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "title \"My App\"\n" +
                "database {\n" +
                "    url \"jdbc:h2:mem:test\"\n" +
                "    pool_size 5\n" +
                "}\n" +
                "tags alpha\n" +
                "tags beta\n"
        );

        final ApplicationConfig config = CONFETTI.map(unit, ApplicationConfig.class);

        assertEquals("My App", config.title);
        assertNotNull(config.database);
        assertEquals("jdbc:h2:mem:test", config.database.url);
        assertEquals(5, config.database.poolSize);
        assertEquals(Arrays.asList("alpha", "beta"), config.tags);
    }

    @Test
    void repeatedBlocks() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "user {\n" +
                "    name Alice\n" +
                "    role admin\n" +
                "}\n" +
                "user {\n" +
                "    name Bob\n" +
                "    role viewer\n" +
                "}"
        );

        final UsersConfig config = CONFETTI.map(unit, UsersConfig.class);

        assertNotNull(config.user);
        assertEquals(2, config.user.size());
        assertEquals("Alice", config.user.get(0).name);
        assertEquals("admin", config.user.get(0).role);
        assertEquals("Bob", config.user.get(1).name);
        assertEquals("viewer", config.user.get(1).role);
    }

    @Test
    void mapField() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "env HOME /home/user\n" +
                "env PATH /usr/bin\n"
        );

        final EnvironmentConfig config = CONFETTI.map(unit, EnvironmentConfig.class);

        assertNotNull(config.env);
        assertEquals("/home/user", config.env.get("HOME"));
        assertEquals("/usr/bin", config.env.get("PATH"));
    }

    @Test
    void booleanVariants() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "server {\n" +
                "    host localhost\n" +
                "    port 443\n" +
                "    debug yes\n" +
                "}"
        );

        final ServerConfig config = CONFETTI.map(unit, ServerConfig.class);

        assertTrue(config.server.debug);
    }

    @Test
    void unknownDirectivesIgnored() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "title hello\n" +
                "unknown_thing 42\n"
        );

        final ApplicationConfig config = CONFETTI.map(unit, ApplicationConfig.class);

        assertEquals("hello", config.title);
    }

    @Test
    void concreteCollectionTypesSupported() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "tags alpha\n" +
                "tags beta\n" +
                "env HOME /home/user\n" +
                "env PATH /usr/bin\n"
        );

        final ConcreteCollectionsConfig config = CONFETTI.map(unit, ConcreteCollectionsConfig.class);

        assertNotNull(config.tags);
        assertEquals(Arrays.asList("alpha", "beta"), config.tags);
        assertNotNull(config.env);
        assertEquals("/home/user", config.env.get("HOME"));
        assertEquals("/usr/bin", config.env.get("PATH"));
    }

    @Test
    void scalarFieldRejectsExtraArguments() {
        final ConfigurationUnit unit = CONFETTI.parse("title hello world");

        final ConfettiMappingException exception = assertThrows(
                ConfettiMappingException.class,
                () -> CONFETTI.map(unit, TitleOnlyConfig.class)
        );

        assertTrue(exception.getMessage().contains("title"));
    }

    @Test
    void listScalarFieldRejectsExtraArguments() {
        final ConfigurationUnit unit = CONFETTI.parse("tags alpha beta");

        ConfettiMappingException exception = assertThrows(
                ConfettiMappingException.class,
                () -> CONFETTI.map(unit, TagsOnlyConfig.class)
        );

        assertTrue(exception.getMessage().contains("tags"));
    }

    @Test
    void mapFieldRejectsExtraArguments() {
        final ConfigurationUnit unit = CONFETTI.parse("env HOME /home/user extra");

        final ConfettiMappingException exception = assertThrows(
                ConfettiMappingException.class,
                () -> CONFETTI.map(unit, EnvironmentConfig.class)
        );

        assertTrue(exception.getMessage().contains("env"));
    }

    @Test
    void nestedObjectReusesExistingInstanceAndMergesRepeatedDirectives() {
        final ConfigurationUnit unit = CONFETTI.parse(
                "server {\n" +
                "    port 8080\n" +
                "}\n" +
                "server {\n" +
                "    host example.com\n" +
                "}\n"
        );

        final NestedDefaultsConfig config = CONFETTI.map(unit, NestedDefaultsConfig.class);

        assertNotNull(config.server);
        assertEquals("example.com", config.server.host);
        assertEquals(8080, config.server.port);
        assertTrue(config.server.debug);
    }

}

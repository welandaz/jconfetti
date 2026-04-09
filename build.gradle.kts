import org.gradle.api.GradleException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "io.github.welandaz"
version = "1.0.0"
description = "A Java parser for the Confetti configuration language"

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the current project version."

    doLast {
        println(project.version)
    }
}

val projectUrl = "https://github.com/welandaz/jconfetti"
val centralPortalUsername = providers.gradleProperty("centralPortalUsername")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_USERNAME"))
val centralPortalPassword = providers.gradleProperty("centralPortalPassword")
    .orElse(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD"))
val signingKey = providers.gradleProperty("signingKey")
    .orElse(providers.environmentVariable("SIGNING_KEY"))
val signingPassword = providers.gradleProperty("signingPassword")
    .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "jconfetti"
            from(components["java"])

            pom {
                name.set("jconfetti")
                description.set(project.description)
                url.set(projectUrl)

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("welandaz")
                        name.set("Iurii Ignatko")
                        url.set("https://github.com/welandaz")
                    }
                }

                scm {
                    url.set(projectUrl)
                    connection.set("scm:git:$projectUrl.git")
                    developerConnection.set("scm:git:ssh://git@github.com/welandaz/jconfetti.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = centralPortalUsername.orNull
                password = centralPortalPassword.orNull
            }
        }
    }
}

signing {
    setRequired {
        gradle.taskGraph.allTasks.any { task ->
            task.name == "publishMavenJavaPublicationToSonatypeRepository" || task.name == "publishToCentral"
        }
    }

    val key = signingKey.orNull
    val password = signingPassword.orNull
    if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
        useInMemoryPgpKeys(key, password)
    }

    sign(publishing.publications["mavenJava"])
}

tasks.register("publishToCentral") {
    group = "publishing"
    description = "Publishes the signed Maven publication to Sonatype Central and requests automatic publication."
    dependsOn("publishMavenJavaPublicationToSonatypeRepository")

    doLast {
        val username = centralPortalUsername.orNull ?: throw GradleException("Missing Central Portal username")
        val password = centralPortalPassword.orNull ?: throw GradleException("Missing Central Portal password")
        val namespace = project.group.toString()
        val authorization = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8))
        val connection = (uri(
            "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/" +
                    "$namespace?publishing_type=automatic"
        ).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $authorization")
            setRequestProperty("Accept", "application/json")
        }

        val responseCode = connection.responseCode
        val responseBody = (if (responseCode >= 400) connection.errorStream else connection.inputStream)
            ?.bufferedReader(StandardCharsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()

        if (responseCode !in 200..299) {
            throw GradleException(
                "Central Portal publication request failed with HTTP $responseCode." +
                        if (responseBody.isBlank()) "" else " Response: $responseBody"
            )
        }

        logger.lifecycle("Published '{}' to Sonatype Central namespace '{}'.", project.name, namespace)
        if (responseBody.isNotBlank()) {
            logger.lifecycle(responseBody)
        }
    }
}

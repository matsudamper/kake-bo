import java.util.Properties
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Strategy
import org.jooq.meta.jaxb.Target

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

buildscript {
    dependencies {
        classpath("org.jooq:jooq-codegen:3.19.7")
        classpath("org.mariadb.jdbc:mariadb-java-client:3.3.3")
        classpath("org.jooq:jooq-meta:3.19.7")
    }
}

kotlin {
    jvm {
        withJava()
    }
    jvmToolchain(17)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                val jooqVersion = "3.19.8"
                implementation("org.jooq:jooq:$jooqVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.create("generateDbCode") {
    doLast {
        val localProperties = Properties().also { properties ->
            val propertiesFile = File("$rootDir/local.properties")
            if (propertiesFile.exists()) {
                properties.load(propertiesFile.inputStream())
            }
        }
        val dbUser = System.getenv("DB_USER")?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty("DB_USER")
            ?: error("DB_USER is not set")
        val dbPass = System.getenv("DB_PASS")?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty("DB_PASS")
            ?: error("DB_PASS is not set")
        GenerationTool.generate(
            Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver("org.mariadb.jdbc.Driver")
                        .withUrl("jdbc:mariadb://localhost:3306/money")
                        .withUser(dbUser)
                        .withPassword(dbPass),
                )
                .withGenerator(
                    Generator()
                        .withName("org.jooq.codegen.KotlinGenerator")
                        .withGenerate(
                            Generate()
                                .withRecords(true),
                        )
                        .withStrategy(
                            Strategy()
                                .withName("org.jooq.codegen.example.JPrefixGeneratorStrategy"),
                        )
                        .withTarget(
                            Target()
                                .withPackageName("net.matsudamper.money.db.schema")
                                .withDirectory("$projectDir/src/jvmMain/kotlin"),
                        )
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.mariadb.MariaDBDatabase")
                                .withInputSchema("money"),
                        ),
                ),
        )
    }
}

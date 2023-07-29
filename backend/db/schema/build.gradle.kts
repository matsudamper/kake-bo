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
        classpath("org.jooq:jooq-codegen:3.18.5")
        classpath("org.mariadb.jdbc:mariadb-java-client:3.1.4")
        classpath("org.jooq:jooq-meta:3.18.5")
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
                api(kotlin("stdlib"))
                api(kotlin("reflect"))

                val jooqVersion = "3.18.5"
                api("org.jooq:jooq:$jooqVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
}

tasks.create("generateDbCode") {
    doLast {
        val localProperties = Properties().also {
            val propertiesFile = File("$rootDir/local.properties")
            if (propertiesFile.exists()) {
                it.load(propertiesFile.inputStream())
            }
        }
        GenerationTool.generate(
            Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver("org.mariadb.jdbc.Driver")
                        .withUrl("jdbc:mariadb://localhost:3306/money")
                        .withUser("root")
                        .withPassword(
                            System.getenv("DB_PASS").takeIf { it.isNotBlank() }
                                ?: localProperties.getProperty("DB_PASS")!!,
                        ),
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

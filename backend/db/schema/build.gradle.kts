import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Target
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

buildscript {
    dependencies {
        classpath("org.jooq:jooq-codegen:3.18.4")
        classpath("org.mariadb.jdbc:mariadb-java-client:3.1.4")
        classpath("org.jooq:jooq-meta:3.1.4")
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

                val jooqVersion = "3.18.4"
                implementation("org.jooq:jooq:$jooqVersion")
//                implementation("org.jooq:jooq-kotlin:$jooqVersion")
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
        val localProperties = Properties().also {
            it.load(File("${rootDir}/local.properties").inputStream())
        }
        GenerationTool.generate(
            Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver("org.mariadb.jdbc.Driver")
                        .withUrl("jdbc:mariadb://localhost:3306/money")
                        .withUser("root")
                        .withPassword(localProperties.getProperty("DB_PASS")!!)
                )
                .withGenerator(
                    Generator()
                        .withName("org.jooq.codegen.KotlinGenerator")
                        .withGenerate(
                            Generate()
                                .withRecords(true)
                        )
                        .withStrategy(
                            Strategy()
                                .withName("org.jooq.codegen.example.JPrefixGeneratorStrategy")
                        )
                        .withTarget(
                            Target()
                                .withPackageName("net.matsudamper.money.db.schema")
                                .withDirectory("$projectDir/src/jvmMain/kotlin")
                        )
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.mariadb.MariaDBDatabase")
                                .withInputSchema("money")
                        )
                )
        )
    }
}
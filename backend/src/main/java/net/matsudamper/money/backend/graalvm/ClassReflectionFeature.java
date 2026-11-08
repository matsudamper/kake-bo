package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.util.List;
import java.util.Set;

/**
 * パッケージ単位では登録できない、ライブラリの個別クラスをリフレクション登録する。
 * <p>
 * reflect-config.json と同じく、クラスパスに無いクラスは警告を出して読み飛ばす。
 */
public class ClassReflectionFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Target target : targets()) {
            Class<?> clazz = findClass(target.className);
            if (clazz != null) {
                register(clazz, target.members);
            }
        }
        // jOOQ の ArrayDataType.getArrayType() が Class.arrayType() を呼ぶ。
        // native image では image に含まれない配列型に対して null が返るため、配列クラスを登録する。
        for (String elementClassName : arrayElementClassNames()) {
            Class<?> elementClass = findClass(elementClassName);
            if (elementClass != null) {
                RuntimeReflection.register(elementClass.arrayType());
            }
        }
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className, false, ClassReflectionFeature.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            System.err.println("[ClassReflectionFeature] Class not found: " + className);
            return null;
        }
    }

    private static void register(Class<?> clazz, Set<ReflectedMember> members) {
        RuntimeReflection.register(clazz);
        try {
            if (members.contains(ReflectedMember.PUBLIC_CONSTRUCTORS)) {
                RuntimeReflection.register(clazz.getConstructors());
            }
            if (members.contains(ReflectedMember.DECLARED_CONSTRUCTORS)) {
                RuntimeReflection.register(clazz.getDeclaredConstructors());
            }
            if (members.contains(ReflectedMember.PUBLIC_METHODS)) {
                RuntimeReflection.register(clazz.getMethods());
            }
            if (members.contains(ReflectedMember.DECLARED_METHODS)) {
                RuntimeReflection.register(clazz.getDeclaredMethods());
            }
            if (members.contains(ReflectedMember.DECLARED_FIELDS)) {
                RuntimeReflection.register(clazz.getDeclaredFields());
            }
        } catch (LinkageError e) {
            // シグネチャに任意依存のクラスが現れると、メンバー一覧の取得自体が失敗する
            System.err.println("[ClassReflectionFeature] Failed to register members: " + clazz.getName() + " (" + e + ")");
        }
    }

    private static List<Target> targets() {
        return List.of(
                new Target("org.mariadb.jdbc.Driver", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS),
                new Target("org.mariadb.jdbc.MariaDbDataSource", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("graphql.GraphQL", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("graphql.execution.AsyncExecutionStrategy", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS),
                new Target("graphql.execution.DataFetcherResult", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("kotlin.reflect.jvm.internal.ReflectionFactoryImpl", ReflectedMember.DECLARED_CONSTRUCTORS),
                new Target("graphql.kickstart.tools.SchemaParser", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("graphql.kickstart.tools.SchemaParserOptions", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("graphql.kickstart.tools.PerFieldConfiguringObjectMapperProvider", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS),
                new Target("com.webauthn4j.data.AuthenticatorTransport", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.webauthn4j.data.attestation.authenticator.AttestedCredentialData", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.webauthn4j.data.client.challenge.DefaultChallenge", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("org.eclipse.angus.mail.imap.IMAPStore", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("org.eclipse.angus.mail.smtp.SMTPTransport", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.sun.mail.imap.IMAPStore", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("java.sql.DriverManager", ReflectedMember.PUBLIC_METHODS),
                new Target("sun.net.www.protocol.https.Handler", ReflectedMember.PUBLIC_CONSTRUCTORS),
                new Target("ch.qos.logback.classic.db.DBAppender", ReflectedMember.DECLARED_CONSTRUCTORS),
                new Target("ch.qos.logback.classic.util.DefaultJoranConfigurator", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.classic.BasicConfigurator", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.core.ConsoleAppender", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.core.FileAppender", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.core.OutputStreamAppender", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.core.encoder.LayoutWrappingEncoder", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.classic.PatternLayout", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.classic.encoder.PatternLayoutEncoder", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("ch.qos.logback.core.pattern.PatternLayoutEncoderBase", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.logstash.logback.encoder.LogstashEncoder", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("io.ktor.network.selector.InterestSuspensionsMap", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.ObjectMapper", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.deser.BeanDeserializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.deser.BeanDeserializerFactory", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.deser.DefaultDeserializationContext$Impl", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.ser.BeanSerializerFactory", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.databind.ser.DefaultSerializerProvider$Impl", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.module.kotlin.KotlinModule", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.module.kotlin.KotlinModule$Builder", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.matsudamper.money.backend.graphql.GraphQlInputFieldDeserializer", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.matsudamper.money.backend.GraphQlRequest", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.matsudamper.money.backend.GraphQlRequest$Extensions", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.matsudamper.money.backend.GraphQlRequest$ClientLibrary", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("net.matsudamper.money.backend.GraphQlRequest$PersistedQuery", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("redis.clients.jedis.DefaultJedisClientConfig", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("redis.clients.jedis.JedisClientConfig", ReflectedMember.PUBLIC_METHODS),
                new Target("redis.clients.jedis.JedisFactory", ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.zaxxer.hikari.HikariConfig", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.zaxxer.hikari.HikariDataSource", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("com.zaxxer.hikari.util.Credentials", ReflectedMember.PUBLIC_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("org.mariadb.jdbc.Configuration", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS),
                new Target("org.mariadb.jdbc.Configuration$Builder", ReflectedMember.DECLARED_CONSTRUCTORS, ReflectedMember.PUBLIC_METHODS, ReflectedMember.DECLARED_METHODS, ReflectedMember.DECLARED_FIELDS)
        );
    }

    private static List<String> arrayElementClassNames() {
        return List.of(
                "java.lang.Boolean",
                "java.lang.Byte",
                "java.lang.Double",
                "java.lang.Float",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Object",
                "java.lang.Short",
                "java.lang.String",
                "java.math.BigDecimal",
                "java.math.BigInteger",
                "java.sql.Date",
                "java.sql.Time",
                "java.sql.Timestamp",
                "java.time.Instant",
                "java.time.LocalDate",
                "java.time.LocalDateTime",
                "java.time.LocalTime",
                "java.time.OffsetDateTime",
                "java.time.OffsetTime",
                "java.time.Year",
                "java.util.UUID",
                "org.jooq.types.UByte",
                "org.jooq.types.UShort",
                "org.jooq.types.UInteger",
                "org.jooq.types.ULong",
                "org.jooq.types.DayToSecond",
                "org.jooq.types.YearToMonth",
                "org.jooq.types.YearToSecond",
                "org.jooq.RowId",
                "org.jooq.Record",
                "org.jooq.Result",
                "org.jooq.JSON",
                "org.jooq.JSONB",
                "org.jooq.XML",
                "org.jooq.Geography",
                "org.jooq.Geometry",
                "org.jooq.Decfloat"
        );
    }

    private enum ReflectedMember {
        PUBLIC_CONSTRUCTORS,
        DECLARED_CONSTRUCTORS,
        PUBLIC_METHODS,
        DECLARED_METHODS,
        DECLARED_FIELDS,
    }

    private static final class Target {
        private final String className;
        private final Set<ReflectedMember> members;

        private Target(String className, ReflectedMember... members) {
            this.className = className;
            this.members = Set.of(members);
        }
    }
}

package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * ライブラリの中で、設定ファイルや他のライブラリからリフレクションで使われるクラスを登録する。
 * 自前のクラスはここに足さず、JacksonModelReflectionFeature などで自動検出する。
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
        registerJooqDataTypeArrays();
    }

    /**
     * jOOQ の ArrayDataType.getArrayType() が Class.arrayType() を呼ぶ。
     * native image では image に含まれない配列型に対して null が返るため、
     * SQLDataType が定義する型の配列クラスを登録する。
     */
    private static void registerJooqDataTypeArrays() {
        Class<?> sqlDataType = findClass("org.jooq.impl.SQLDataType");
        if (sqlDataType == null) {
            throw new IllegalStateException("org.jooq.impl.SQLDataType がクラスパスに無い");
        }
        for (Field field : sqlDataType.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!(field.getGenericType() instanceof ParameterizedType dataType)) continue;
            Class<?> valueType = rawClass(dataType.getActualTypeArguments()[0]);
            if (valueType != null) {
                RuntimeReflection.register(valueType.arrayType());
            }
        }
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getRawType());
        }
        return null;
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

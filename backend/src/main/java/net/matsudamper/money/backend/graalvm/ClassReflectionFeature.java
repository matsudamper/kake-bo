package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ライブラリや設定ファイルからリフレクションで使われるクラスを、パッケージ直下の走査で登録する。
 * サブパッケージまで含めると登録数が膨らむので、直下だけを対象にする。
 */
public class ClassReflectionFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (String targetPackage : targetPackages()) {
            List<String> classNames = PackageClassScanner.findClassNames(targetPackage, false);
            if (classNames.isEmpty()) {
                System.err.println("[ClassReflectionFeature] No classes found: " + targetPackage);
            }
            for (String className : classNames) {
                Class<?> clazz = loadWithoutInitialization(className);
                if (clazz != null) {
                    registerAllMembers(clazz);
                }
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
        Class<?> sqlDataType = loadWithoutInitialization("org.jooq.impl.SQLDataType");
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

    /**
     * 実行時初期化を指定したクラスをビルド時に初期化させないよう、初期化せずに読み込む。
     */
    private static Class<?> loadWithoutInitialization(String className) {
        try {
            return Class.forName(className, false, ClassReflectionFeature.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            System.err.println("[ClassReflectionFeature] Class not loaded: " + className + " (" + e + ")");
            return null;
        }
    }

    private static void registerAllMembers(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        try {
            RuntimeReflection.register(clazz.getDeclaredConstructors());
            RuntimeReflection.register(clazz.getDeclaredMethods());
            RuntimeReflection.register(clazz.getMethods());
            RuntimeReflection.register(clazz.getDeclaredFields());
        } catch (LinkageError e) {
            // シグネチャに任意依存のクラスが現れると、メンバー一覧の取得自体が失敗する
            System.err.println("[ClassReflectionFeature] Failed to register members: " + clazz.getName() + " (" + e + ")");
        }
    }

    private static List<String> targetPackages() {
        return List.of(
                "net.matsudamper.money.backend",
                "net.matsudamper.money.backend.graphql",
                "java.sql",
                "sun.net.www.protocol.https",
                "kotlin.reflect.jvm.internal",
                "io.ktor.network.selector",
                "org.mariadb.jdbc",
                "com.zaxxer.hikari",
                "com.zaxxer.hikari.util",
                "redis.clients.jedis",
                "graphql",
                "graphql.execution",
                "graphql.kickstart.tools",
                "org.eclipse.angus.mail.imap",
                "org.eclipse.angus.mail.smtp",
                "com.sun.mail.imap",
                "ch.qos.logback.classic",
                "ch.qos.logback.classic.db",
                "ch.qos.logback.classic.encoder",
                "ch.qos.logback.classic.util",
                "ch.qos.logback.core",
                "ch.qos.logback.core.encoder",
                "ch.qos.logback.core.pattern",
                "net.logstash.logback.encoder",
                "com.fasterxml.jackson.databind",
                "com.fasterxml.jackson.databind.deser",
                "com.fasterxml.jackson.databind.ser",
                "com.fasterxml.jackson.module.kotlin",
                "com.fasterxml.jackson.datatype.jsr310",
                "com.fasterxml.jackson.datatype.jsr310.deser",
                "com.fasterxml.jackson.datatype.jsr310.ser"
        );
    }
}

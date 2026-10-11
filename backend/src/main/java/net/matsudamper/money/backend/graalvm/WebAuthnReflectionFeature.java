package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.util.List;

/**
 * webauthn4j は Jackson 3 で CBOR/JSON を読む。
 * {@code @JsonCreator} や {@code TypeReference} の総称は実行時リフレクションなので、
 * データクラスを登録しないと native image でだけセキュリティキーの登録・検証に失敗する。
 */
public class WebAuthnReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "com.webauthn4j";
    }

    @Override
    public void beforeAnalysis(Feature.BeforeAnalysisAccess access) {
        super.beforeAnalysis(access);
        for (String className : extraClassNames()) {
            try {
                register(Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(className + " がクラスパスに無い", e);
            }
        }
    }

    @Override
    protected void register(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
        RuntimeReflection.register(clazz.getFields());
    }

    /**
     * Jackson が注釈の値とコレクション型のコンストラクタをリフレクションで取る。
     * webauthn4j 本体の走査には含まれない。
     */
    private static List<String> extraClassNames() {
        return List.of(
                "com.fasterxml.jackson.annotation.JsonCreator",
                "com.fasterxml.jackson.annotation.JsonProperty",
                "com.fasterxml.jackson.annotation.JsonIgnore",
                "com.fasterxml.jackson.annotation.JsonValue",
                "com.fasterxml.jackson.annotation.JsonTypeInfo",
                "com.fasterxml.jackson.annotation.JsonTypeInfo$Id",
                "com.fasterxml.jackson.annotation.JsonTypeInfo$As",
                "com.fasterxml.jackson.annotation.JsonSubTypes",
                "com.fasterxml.jackson.annotation.JsonSubTypes$Type",
                "com.fasterxml.jackson.annotation.JsonInclude",
                "com.fasterxml.jackson.annotation.JsonInclude$Include",
                "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
                "com.fasterxml.jackson.annotation.JsonFormat",
                "com.fasterxml.jackson.annotation.JsonFormat$Shape",
                "java.util.ArrayList",
                "java.util.LinkedHashMap",
                "java.util.HashMap",
                "java.util.LinkedHashSet",
                "java.util.HashSet",
                "java.util.TreeMap",
                "java.math.BigInteger"
        );
    }
}

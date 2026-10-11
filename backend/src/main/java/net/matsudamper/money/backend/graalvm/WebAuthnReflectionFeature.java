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
    protected List<String> extraPackages() {
        return List.of("com.fasterxml.jackson.annotation");
    }

    @Override
    public void beforeAnalysis(Feature.BeforeAnalysisAccess access) {
        super.beforeAnalysis(access);
        for (String className : jdkClassNames()) {
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
     * Jackson が List や Map の実装をリフレクションで作る。
     * JDK のクラスはパッケージ走査の対象外。
     */
    private static List<String> jdkClassNames() {
        return List.of(
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

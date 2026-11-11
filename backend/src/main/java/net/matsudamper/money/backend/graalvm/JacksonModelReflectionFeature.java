package net.matsudamper.money.backend.graalvm;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Jackson で読み書きする自前のクラスを、バックエンドのパッケージから自動で探して登録する。
 * Jackson のアノテーションを持つクラスと、Jackson のシリアライザ・デシリアライザが対象。
 * DTO を足すたびに登録を追記しなくて済むようにしている。
 */
public class JacksonModelReflectionFeature implements Feature {

    private static final String TARGET_PACKAGE = "net.matsudamper.money.backend";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        List<String> classNames = PackageClassScanner.findClassNames(TARGET_PACKAGE);
        // 走査に失敗しても native-image のビルドは通り、実行時に初めて落ちるので、ここで止める
        if (classNames.isEmpty()) {
            throw new IllegalStateException(TARGET_PACKAGE + " にクラスが 1 つも無い。パッケージを移したか、走査に失敗している");
        }
        for (String className : classNames) {
            Class<?> clazz = loadWithoutInitialization(className);
            if (clazz != null && isJacksonTarget(clazz)) {
                register(clazz);
            }
        }
    }

    private static boolean isJacksonTarget(Class<?> clazz) {
        try {
            return JsonDeserializer.class.isAssignableFrom(clazz)
                    || JsonSerializer.class.isAssignableFrom(clazz)
                    || hasJacksonAnnotation(clazz);
        } catch (LinkageError e) {
            // シグネチャに任意依存のクラスが現れると、メンバー一覧の取得自体が失敗する
            System.err.println("[JacksonModelReflectionFeature] Failed to inspect: " + clazz.getName() + " (" + e + ")");
            return false;
        }
    }

    private static boolean hasJacksonAnnotation(Class<?> clazz) {
        Stream<AnnotatedElement> elements = Stream.of(
                Stream.<AnnotatedElement>of(clazz),
                Arrays.stream(clazz.getDeclaredConstructors()),
                Arrays.stream(clazz.getDeclaredFields()),
                Arrays.stream(clazz.getDeclaredMethods())
        ).flatMap(stream -> stream);
        Stream<Annotation> parameterAnnotations = Arrays.stream(clazz.getDeclaredConstructors())
                .map(Constructor::getParameterAnnotations)
                .flatMap(Arrays::stream)
                .flatMap(Arrays::stream);
        return Stream.concat(elements.flatMap(element -> Arrays.stream(element.getDeclaredAnnotations())), parameterAnnotations)
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(JacksonAnnotation.class));
    }

    /**
     * 実行時初期化を指定したクラスをビルド時に初期化させないよう、初期化せずに読み込む。
     */
    private static Class<?> loadWithoutInitialization(String className) {
        try {
            return Class.forName(className, false, JacksonModelReflectionFeature.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            System.err.println("[JacksonModelReflectionFeature] Class not loaded: " + className + " (" + e + ")");
            return null;
        }
    }

    private static void register(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
    }
}

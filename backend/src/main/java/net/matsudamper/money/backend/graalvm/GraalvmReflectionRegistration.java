package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * native-image 用リフレクション登録の共通処理。
 * 継承メンバーまで登録すると jOOQ Record などで不要に分析・メタデータが膨らむため、宣言メンバーに限定する。
 */
final class GraalvmReflectionRegistration {

    private GraalvmReflectionRegistration() {
    }

    static void registerDeclaredMembers(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
    }
}

package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * GraphQL の生成モデルが持つ ID 型を一括登録する。
 * input object は kickstart が Jackson で組み立てるので、中の ID 型のコンストラクタが登録されていないと
 * native バイナリでだけ InvalidDefinitionException になる。
 */
public class SharedElementReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "net.matsudamper.money.element";
    }

    @Override
    protected void register(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
    }
}

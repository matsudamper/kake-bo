package net.matsudamper.money.backend.graalvm;

/**
 * graphql-kickstart-tools がスキーマのフィールドと対応付けるリゾルバ実装を一括登録する。
 * reflect-config.json に手で並べると、リゾルバを足したときに登録漏れが起き、native バイナリでだけ解決に失敗する。
 */
public class GraphqlResolverReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "net.matsudamper.money.backend.graphql.resolver";
    }

    @Override
    protected void register(Class<?> clazz) {
        GraalvmReflectionRegistration.registerDeclaredMembers(clazz);
    }
}

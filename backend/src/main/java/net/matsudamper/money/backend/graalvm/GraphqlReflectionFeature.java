package net.matsudamper.money.backend.graalvm;

/**
 * GraphQL コード生成の入力型・接続型などをリフレクション登録する。
 * リゾルバ IF は kickstart が {@code backend.graphql.resolver} の実装クラスを見るため登録しない。
 */
public class GraphqlReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "net.matsudamper.money.graphql.model";
    }

    @Override
    protected void register(Class<?> clazz) {
        GraalvmReflectionRegistration.registerDeclaredMembers(clazz);
    }
}

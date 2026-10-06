package net.matsudamper.money.backend.graalvm;

/**
 * jOOQ が生成した Record クラスを、テーブル追加のたびに reflect-config.json へ追記しなくて済むよう一括登録する。
 */
public class JooqRecordReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "net.matsudamper.money.db.schema.tables.records";
    }

    @Override
    protected void register(Class<?> clazz) {
        GraalvmReflectionRegistration.registerDeclaredMembers(clazz);
    }
}

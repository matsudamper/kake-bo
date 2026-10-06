package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * GraalVM Feature that registers all GraphQL codegen-generated classes for reflection,
 * so that graphql-kickstart-tools can resolve fields and methods at runtime.
 */
public class GraphqlReflectionFeature extends PackageReflectionFeature {

    @Override
    protected String targetPackage() {
        return "net.matsudamper.money.graphql.model";
    }

    @Override
    protected void register(Class<?> clazz) {
        RuntimeReflection.register(clazz);
        RuntimeReflection.register(clazz.getDeclaredConstructors());
        RuntimeReflection.register(clazz.getDeclaredMethods());
        RuntimeReflection.register(clazz.getDeclaredFields());
    }
}

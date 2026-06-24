package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GraalVM Feature that registers all GraphQL codegen-generated classes for reflection.
 * <p>
 * Scans the application classpath at native-image build time to discover all classes
 * under {@code net.matsudamper.money.graphql.model} and registers them for reflection,
 * so that graphql-kickstart-tools can resolve fields and methods at runtime.
 */
public class GraphqlReflectionFeature implements Feature {

    private static final String TARGET_PACKAGE = "net.matsudamper.money.graphql.model";
    private static final String TARGET_PATH = TARGET_PACKAGE.replace('.', '/');

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        List<String> classNames = new ArrayList<>();
        try {
            collectClassNames(access, classNames);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan package: " + TARGET_PACKAGE, e);
        }
        Collections.sort(classNames);
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                RuntimeReflection.register(clazz);
                RuntimeReflection.register(clazz.getDeclaredConstructors());
                RuntimeReflection.register(clazz.getDeclaredMethods());
                RuntimeReflection.register(clazz.getDeclaredFields());
            } catch (ClassNotFoundException e) {
                System.err.println("[GraphqlReflectionFeature] Class not found: " + className);
            }
        }
    }

    private void collectClassNames(BeforeAnalysisAccess access, List<String> classNames) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = GraphqlReflectionFeature.class.getClassLoader();
        }
        var resources = classLoader.getResources(TARGET_PATH);
        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            URI uri = resource.toURI();
            if ("file".equals(uri.getScheme())) {
                scanDirectory(Paths.get(uri), classNames);
            } else if ("jar".equals(uri.getScheme())) {
                scanJar(uri, classNames);
            }
        }
    }

    private void scanDirectory(Path dir, List<String> classNames) throws IOException {
        if (!Files.isDirectory(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".class")) {
                    String relativePath = dir.relativize(file).toString();
                    String className = TARGET_PACKAGE + "." +
                            relativePath.replace(File.separatorChar, '.').replace('/', '.')
                                    .substring(0, relativePath.length() - ".class".length());
                    classNames.add(className);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanJar(URI jarUri, List<String> classNames) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
            Path packagePath = fs.getPath(TARGET_PATH);
            if (!Files.isDirectory(packagePath)) return;
            Files.walkFileTree(packagePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fullPath = file.toString();
                    if (fullPath.startsWith("/")) fullPath = fullPath.substring(1);
                    if (fullPath.endsWith(".class")) {
                        String className = fullPath.replace('/', '.')
                                .substring(0, fullPath.length() - ".class".length());
                        classNames.add(className);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}

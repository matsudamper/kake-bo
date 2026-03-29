package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * GraalVM Feature that registers project classes used by GraphQL/JOOQ for reflection.
 */
public final class GraphqlReflectionFeature implements Feature {

    private static final String[] TARGET_PACKAGES = {
            "net.matsudamper.money.backend",
            "net.matsudamper.money.db.schema",
            "net.matsudamper.money.graphql.model"
    };

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = GraphqlReflectionFeature.class.getClassLoader();
        }

        Set<String> classNames = new TreeSet<>();
        try {
            for (String targetPackage : TARGET_PACKAGES) {
                collectClassNames(classLoader, targetPackage, classNames);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register reflection metadata", e);
        }

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                RuntimeReflection.register(clazz);
                RuntimeReflection.register(clazz.getDeclaredConstructors());
                RuntimeReflection.register(clazz.getDeclaredMethods());
                RuntimeReflection.register(clazz.getDeclaredFields());
            } catch (ClassNotFoundException e) {
                System.err.println("[GraphqlReflectionFeature] Class not found: " + className);
            }
        }
    }

    private void collectClassNames(ClassLoader classLoader, String targetPackage, Set<String> classNames)
            throws IOException, URISyntaxException {
        String targetPath = targetPackage.replace('.', '/');
        var resources = classLoader.getResources(targetPath);
        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            URI uri = resource.toURI();
            if ("file".equals(uri.getScheme())) {
                scanDirectory(Paths.get(uri), targetPackage, classNames);
            } else if ("jar".equals(uri.getScheme())) {
                scanJar(uri, targetPath, classNames);
            }
        }
    }

    private void scanDirectory(Path dir, String targetPackage, Set<String> classNames) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".class")) {
                    String relativePath = dir.relativize(file).toString();
                    String className = targetPackage + "." +
                            relativePath.replace(File.separatorChar, '.').replace('/', '.')
                                    .substring(0, relativePath.length() - ".class".length());
                    classNames.add(className);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanJar(URI jarUri, String targetPath, Set<String> classNames) throws IOException {
        FileSystem fs;
        boolean createdNew = false;
        try {
            fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap());
            createdNew = true;
        } catch (FileSystemAlreadyExistsException ignored) {
            fs = FileSystems.getFileSystem(jarUri);
        }

        try (FileSystem ignored = createdNew ? fs : null) {
            Path packagePath = fs.getPath(targetPath);
            if (!Files.isDirectory(packagePath)) {
                return;
            }
            Files.walkFileTree(packagePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fullPath = file.toString();
                    if (fullPath.startsWith("/")) {
                        fullPath = fullPath.substring(1);
                    }
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

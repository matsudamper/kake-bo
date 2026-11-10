package net.matsudamper.money.backend.graalvm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * クラスパスから、パッケージとそのサブパッケージに含まれるクラス名を集める。
 */
final class PackageClassScanner {

    private PackageClassScanner() {
    }

    static List<String> findClassNames(String targetPackage) {
        List<String> classNames = new ArrayList<>();
        try {
            collectFromClassPath(targetPackage, classNames);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to scan package: " + targetPackage, e);
        }
        Collections.sort(classNames);
        return classNames;
    }

    private static void collectFromClassPath(String targetPackage, List<String> classNames)
            throws IOException, URISyntaxException {
        String targetPath = targetPackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PackageClassScanner.class.getClassLoader();
        }
        var resources = classLoader.getResources(targetPath);
        while (resources.hasMoreElements()) {
            URI uri = resources.nextElement().toURI();
            if ("file".equals(uri.getScheme())) {
                collectFromDirectory(targetPackage, Paths.get(uri), classNames);
            } else if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    collectFromDirectory(targetPackage, fs.getPath(targetPath), classNames);
                }
            }
        }
    }

    private static void collectFromDirectory(String targetPackage, Path packageDir, List<String> classNames)
            throws IOException {
        if (!Files.isDirectory(packageDir)) return;
        try (Stream<Path> files = Files.walk(packageDir)) {
            for (Path file : files.toList()) {
                String relativePath = packageDir.relativize(file).toString();
                if (!relativePath.endsWith(".class")) continue;
                String relativeClassName = relativePath
                        .substring(0, relativePath.length() - ".class".length())
                        .replace(file.getFileSystem().getSeparator(), ".");
                classNames.add(targetPackage + "." + relativeClassName);
            }
        }
    }
}

package net.matsudamper.money.backend.graalvm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * クラスパスと JDK のモジュールから、パッケージに含まれるクラス名を集める。
 */
final class PackageClassScanner {

    private PackageClassScanner() {
    }

    static List<String> findClassNames(String targetPackage, boolean includeSubPackages) {
        List<String> classNames = new ArrayList<>();
        try {
            collectFromClassPath(targetPackage, includeSubPackages, classNames);
            collectFromJdkModules(targetPackage, includeSubPackages, classNames);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to scan package: " + targetPackage, e);
        }
        Collections.sort(classNames);
        return classNames;
    }

    private static void collectFromClassPath(String targetPackage, boolean includeSubPackages, List<String> classNames)
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
                collectFromDirectory(targetPackage, Paths.get(uri), includeSubPackages, classNames);
            } else if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    collectFromDirectory(targetPackage, fs.getPath(targetPath), includeSubPackages, classNames);
                }
            }
        }
    }

    /**
     * JDK のクラスは ClassLoader.getResources でパッケージのディレクトリが返らないので、jrt ファイルシステムから探す。
     */
    private static void collectFromJdkModules(String targetPackage, boolean includeSubPackages, List<String> classNames)
            throws IOException {
        FileSystem jrt;
        try {
            jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        } catch (FileSystemNotFoundException e) {
            return;
        }
        Path packageLinks = jrt.getPath("/packages", targetPackage);
        if (!Files.isDirectory(packageLinks)) return;
        String targetPath = targetPackage.replace('.', '/');
        try (Stream<Path> modules = Files.list(packageLinks)) {
            for (Path module : modules.toList()) {
                Path packageDir = jrt.getPath("/modules", module.getFileName().toString(), targetPath);
                collectFromDirectory(targetPackage, packageDir, includeSubPackages, classNames);
            }
        }
    }

    private static void collectFromDirectory(String targetPackage, Path packageDir, boolean includeSubPackages, List<String> classNames)
            throws IOException {
        if (!Files.isDirectory(packageDir)) return;
        int maxDepth = includeSubPackages ? Integer.MAX_VALUE : 1;
        try (Stream<Path> files = Files.walk(packageDir, maxDepth)) {
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

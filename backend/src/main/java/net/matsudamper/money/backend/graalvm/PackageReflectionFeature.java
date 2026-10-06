package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;

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
 * native-image ビルド時にクラスパスを走査し、指定パッケージ配下の全クラスをリフレクション登録する Feature の基底クラス。
 * <p>
 * reflect-config.json はクラス名のワイルドカード指定ができないため、パッケージ単位で登録したい場合に使う。
 */
public abstract class PackageReflectionFeature implements Feature {

    protected abstract String targetPackage();

    protected abstract void register(Class<?> clazz);

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        String targetPackage = targetPackage();
        List<String> classNames = new ArrayList<>();
        try {
            collectClassNames(targetPackage, classNames);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan package: " + targetPackage, e);
        }
        // 走査に失敗しても native-image のビルドは通り、実行時に初めて落ちるので、ここで止める
        if (classNames.isEmpty()) {
            throw new IllegalStateException(targetPackage + " にクラスが 1 つも無い。パッケージを移したか、走査に失敗している");
        }
        Collections.sort(classNames);
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (!shouldRegisterClass(clazz)) {
                    continue;
                }
                register(clazz);
            } catch (ClassNotFoundException e) {
                System.err.println("[" + getClass().getSimpleName() + "] Class not found: " + className);
            }
        }
    }

    /**
     * 走査で見つかったクラスをリフレクション登録するか。デフォルトは具象クラスのみ。
     */
    protected boolean shouldRegisterClass(Class<?> clazz) {
        return !clazz.isInterface() && !clazz.isSynthetic();
    }

    private void collectClassNames(String targetPackage, List<String> classNames) throws IOException, URISyntaxException {
        String targetPath = targetPackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PackageReflectionFeature.class.getClassLoader();
        }
        var resources = classLoader.getResources(targetPath);
        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            URI uri = resource.toURI();
            if ("file".equals(uri.getScheme())) {
                scanDirectory(targetPackage, Paths.get(uri), classNames);
            } else if ("jar".equals(uri.getScheme())) {
                scanJar(targetPath, uri, classNames);
            }
        }
    }

    private void scanDirectory(String targetPackage, Path dir, List<String> classNames) throws IOException {
        if (!Files.isDirectory(dir)) return;
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

    private void scanJar(String targetPath, URI jarUri, List<String> classNames) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
            Path packagePath = fs.getPath(targetPath);
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

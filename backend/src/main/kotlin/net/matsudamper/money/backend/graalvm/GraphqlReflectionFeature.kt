package net.matsudamper.money.backend.graalvm

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection

/**
 * GraphQL codegen が生成したクラスを Native Image 向けに reflection 登録する Feature。
 *
 * native-image build 時にアプリケーションクラスパスを走査し、
 * `net.matsudamper.money.graphql.model` 配下のクラスを収集して reflection 登録する。
 * これにより graphql-kickstart-tools が実行時にフィールドやメソッドを解決できる。
 */
@Suppress("unused")
class GraphqlReflectionFeature : Feature {

    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        val classNames = mutableListOf<String>()
        try {
            collectClassNames(classNames)
        } catch (e: Exception) {
            throw RuntimeException("Failed to scan package: $TARGET_PACKAGE", e)
        }

        classNames.sort()
        classNames.forEach { className ->
            try {
                val clazz = Class.forName(className)
                RuntimeReflection.register(clazz)
                RuntimeReflection.register(*clazz.declaredConstructors)
                RuntimeReflection.register(*clazz.declaredMethods)
                RuntimeReflection.register(*clazz.declaredFields)
            } catch (_: ClassNotFoundException) {
                System.err.println("[GraphqlReflectionFeature] Class not found: $className")
            }
        }
    }

    @Throws(IOException::class, URISyntaxException::class)
    private fun collectClassNames(classNames: MutableList<String>) {
        val classLoader = Thread.currentThread().contextClassLoader ?: GraphqlReflectionFeature::class.java.classLoader
        val resources = classLoader.getResources(TARGET_PATH)
        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            val uri = resource.toURI()
            when (uri.scheme) {
                "file" -> scanDirectory(Paths.get(uri), classNames)
                "jar" -> scanJar(uri, classNames)
            }
        }
    }

    @Throws(IOException::class)
    private fun scanDirectory(dir: Path, classNames: MutableList<String>) {
        if (!Files.isDirectory(dir)) {
            return
        }

        Files.walkFileTree(
            dir,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val fileName = file.fileName.toString()
                    if (fileName.endsWith(".class")) {
                        val relativePath = dir.relativize(file).toString()
                        val className =
                            "$TARGET_PACKAGE." +
                                relativePath
                                    .replace(File.separatorChar, '.')
                                    .replace('/', '.')
                                    .removeSuffix(".class")
                        classNames.add(className)
                    }
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    @Throws(IOException::class)
    private fun scanJar(jarUri: URI, classNames: MutableList<String>) {
        FileSystems.newFileSystem(jarUri, emptyMap<String, Any>()).use { fileSystem: FileSystem ->
            val packagePath = fileSystem.getPath(TARGET_PATH)
            if (!Files.isDirectory(packagePath)) {
                return
            }

            Files.walkFileTree(
                packagePath,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val fullPath = file.toString().removePrefix("/")
                        if (fullPath.endsWith(".class")) {
                            val className = fullPath.replace('/', '.').removeSuffix(".class")
                            classNames.add(className)
                        }
                        return FileVisitResult.CONTINUE
                    }
                },
            )
        }
    }

    private companion object {
        private const val TARGET_PACKAGE = "net.matsudamper.money.graphql.model"
        private const val TARGET_PATH = "net/matsudamper/money/graphql/model"
    }
}

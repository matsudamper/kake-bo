package net.matsudamper.money.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.TestedExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure

fun DependencyHandlerScope.implementation(
    artifact: Any,
) {
    add("implementation", artifact)
}
fun DependencyHandlerScope.testRuntimeOnly(
    artifact: Any,
) {
    add("testRuntimeOnly", artifact)
}
fun DependencyHandlerScope.testImplementation(
    artifact: Any,
) {
    add("testImplementation", artifact)
}

fun DependencyHandlerScope.androidTestImplementation(
    artifact: Any,
) {
    add("androidTestImplementation", artifact)
}

fun DependencyHandlerScope.coreLibraryDesugaring(
    artifact: Any,
) {
    add("coreLibraryDesugaring", artifact)
}

fun DependencyHandlerScope.debugImplementation(
    artifact: Any,
) {
    add("debugImplementation", artifact)
}

fun Project.android(action: TestedExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.androidApplication(action: BaseAppModuleExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.androidCommon(action: CommonExtension<*, *, *, *, *, *>.() -> Unit) {
    val extension = extensions.findByName("android") as CommonExtension<*, *, *, *, *, *>
    action(extension)
}

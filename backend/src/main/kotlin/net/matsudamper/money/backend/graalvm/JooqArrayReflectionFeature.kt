package net.matsudamper.money.backend.graalvm

import java.lang.reflect.Array
import java.lang.reflect.Modifier
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection

/**
 * jOOQ の built-in 型が参照する配列クラスを Native Image 向けに登録する Feature。
 *
 * jOOQ 3.21 は `org.jooq.impl.SQLDataType` の初期化中に配列型を eager に生成する。
 * GraalVM native-image では、image の type universe に含まれていない配列型に対する
 * `Class.arrayType()` が `null` になるため、build-time に
 * `org.jooq.DataType` 定義を走査して配列クラスを登録する。
 */
@Suppress("unused")
class JooqArrayReflectionFeature : Feature {

    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        val registeredArrayTypes = mutableSetOf<Class<*>>()

        try {
            val sqlDataTypeClass = Class.forName(SQL_DATA_TYPE_CLASS)
            val dataTypeClass = Class.forName(DATA_TYPE_CLASS)
            val getTypeMethod = dataTypeClass.getMethod("getType")

            sqlDataTypeClass.declaredFields.forEach { field ->
                if (!Modifier.isStatic(field.modifiers) || !dataTypeClass.isAssignableFrom(field.type)) {
                    return@forEach
                }

                try {
                    field.trySetAccessible()
                    val dataType = field.get(null) ?: return@forEach
                    val type = getTypeMethod.invoke(dataType) as? Class<*> ?: return@forEach
                    if (type.isArray) {
                        return@forEach
                    }

                    val arrayType = Array.newInstance(type, 0).javaClass
                    if (registeredArrayTypes.add(arrayType)) {
                        RuntimeReflection.register(arrayType)
                    }
                } catch (e: ReflectiveOperationException) {
                    throw RuntimeException("Failed to register jOOQ array type for field: ${field.name}", e)
                }
            }
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException("Failed to inspect jOOQ SQLDataType definitions", e)
        }
    }

    private companion object {
        private const val SQL_DATA_TYPE_CLASS = "org.jooq.impl.SQLDataType"
        private const val DATA_TYPE_CLASS = "org.jooq.DataType"
    }
}

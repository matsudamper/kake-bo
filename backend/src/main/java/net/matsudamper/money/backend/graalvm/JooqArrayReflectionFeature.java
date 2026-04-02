package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Registers the array classes referenced by jOOQ built-in data types.
 * <p>
 * jOOQ 3.21 eagerly creates array data types while initialising org.jooq.impl.SQLDataType.
 * GraalVM native-image returns {@code null} from {@link Class#arrayType()} when the
 * array class is not part of the image type universe, so we discover all built-in
 * org.jooq.DataType definitions at build time and register their array classes.
 */
public final class JooqArrayReflectionFeature implements Feature {

    private static final String SQL_DATA_TYPE_CLASS = "org.jooq.impl.SQLDataType";
    private static final String DATA_TYPE_CLASS = "org.jooq.DataType";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Set<Class<?>> registeredArrayTypes = new HashSet<>();

        try {
            Class<?> sqlDataTypeClass = Class.forName(SQL_DATA_TYPE_CLASS);
            Class<?> dataTypeClass = Class.forName(DATA_TYPE_CLASS);
            Method getTypeMethod = dataTypeClass.getMethod("getType");

            for (Field field : sqlDataTypeClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !dataTypeClass.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object dataType = field.get(null);
                    if (dataType == null) {
                        continue;
                    }

                    Class<?> type = (Class<?>) getTypeMethod.invoke(dataType);
                    if (type == null || type.isArray()) {
                        continue;
                    }

                    Class<?> arrayType = Array.newInstance(type, 0).getClass();
                    if (registeredArrayTypes.add(arrayType)) {
                        RuntimeReflection.register(arrayType);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to register jOOQ array type for field: " + field.getName(), e);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inspect jOOQ SQLDataType definitions", e);
        }
    }
}

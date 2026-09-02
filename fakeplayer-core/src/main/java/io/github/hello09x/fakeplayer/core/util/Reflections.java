package io.github.hello09x.fakeplayer.core.util;

import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Reflections {

    public static @Nullable Field getFirstFieldByType(
            @NotNull Class<?> clazz,
            @NotNull Class<?> fieldType,
            boolean includeStatic
    ) {
        for (var field : clazz.getDeclaredFields()) {
            if (includeStatic ^ Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() == fieldType) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }


    public static @N Field getFistFieldByTypeIncludeParent(
            @NotNull Class<?> clazz,
            @NotNull Class<?> fieldType
    ) {
        for (var field : clazz.getDeclaredFields()) {
            if (field.getType() == fieldType) {
                field.setAccessible(true);
                return field;
            }
        }
        var superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        return getFistFieldByTypeIncludeParent(superclass, fieldType);
    }

    public static @Nullable Field getFirstFieldByAssignFromType(
            @NotNull Class<?> clazz,
            @NotNull Class<?> fieldType,
            boolean includeStatic
    ) {
        for (var field : clazz.getDeclaredFields()) {
            if (includeStatic ^ Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (fieldType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    public static @NotNull Object getHandle(@NotNull Object object) {
        return invokeNoArgs(object, "getHandle");
    }

    public static @NotNull Object getServer(@NotNull Object object) {
        return invokeNoArgs(object, "getServer");
    }

    /**
     * Resolves the NMS server without relying solely on CraftServer#getServer().
     * Some Paper forks change that method's return value, while the backing
     * MinecraftServer field remains available.
     */
    public static @NotNull Object getMinecraftServer(
            @NotNull Object server,
            @NotNull Class<?> minecraftServerClass
    ) {
        var field = getFistFieldByTypeIncludeParent(server.getClass(), minecraftServerClass);
        if (field != null) {
            try {
                return field.get(server);
            } catch (IllegalAccessException ignored) {
                // Fall back to the public accessor below.
            }
        }
        return getServer(server);
    }

    private static @NotNull Object invokeNoArgs(@NotNull Object object, @NotNull String methodName) {
        try {
            Method method = object.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(object);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to invoke " + object.getClass().getName() + "#" + methodName + "()",
                    e
            );
        }
    }

}

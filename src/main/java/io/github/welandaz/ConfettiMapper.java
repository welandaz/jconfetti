package io.github.welandaz;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

final class ConfettiMapper {

    private ConfettiMapper() {
    }

    static <T> T map(final ConfigurationUnit configurationUnit, final Class<T> clazz) {
        try {
            final T target = clazz.getDeclaredConstructor().newInstance();

            mapDirectives(configurationUnit.directives(), target);

            return target;
        } catch (final ReflectiveOperationException e) {
            throw new ConfettiMappingException("Failed to map configuration to " + clazz.getName(), e);
        }
    }

    private static void mapDirectives(final List<Directive> directives, final Object target)
            throws ReflectiveOperationException {
        final Map<String, Field> fieldMap = buildFieldMap(target.getClass());

        for (final Directive directive : directives) {
            final Field field = fieldMap.get(directive.name());
            if (field == null) {
                continue;
            }

            field.setAccessible(true);

            final Class<?> fieldType = field.getType();
            if (List.class.isAssignableFrom(fieldType)) {
                mapListField(field, directive, target);
                continue;
            }

            if (Map.class.isAssignableFrom(fieldType)) {
                mapMapField(field, directive, target);
                continue;
            }

            if (directive.hasSubdirectives()) {
                mapNestedField(field, directive, target);
                continue;
            }

            if (isScalar(fieldType)) {
                field.set(target, convert(readSingleValue(field, directive), fieldType));
                continue;
            }

            throw mappingError(field, "requires subdirectives");
        }
    }

    private static void mapListField(final Field field, final Directive directive, final Object target)
            throws ReflectiveOperationException {
        final Class<?> elementType = resolveTypeArgument(field, 0);
        final List<Object> list = getOrCreateContainer(field, target, ArrayList::new);

        if (directive.hasSubdirectives()) {
            if (isScalar(elementType)) {
                throw mappingError(field, "expects scalar values without subdirectives");
            }

            validateNestedDirective(field, directive);

            final Object nested = elementType.getDeclaredConstructor().newInstance();
            mapDirectives(directive.subdirectives(), nested);
            list.add(nested);
            return;
        }

        if (!isScalar(elementType)) {
            throw mappingError(field, "expects subdirectives for element type " + elementType.getName());
        }

        list.add(convert(readSingleValue(field, directive), elementType));
    }

    private static void mapNestedField(final Field field, final Directive directive, final Object target)
            throws ReflectiveOperationException {
        validateNestedDirective(field, directive);

        Object nested = field.get(target);
        if (nested == null) {
            nested = field.getType().getDeclaredConstructor().newInstance();
            field.set(target, nested);
        }

        mapDirectives(directive.subdirectives(), nested);
    }

    private static void mapMapField(Field field, Directive directive, Object target) throws ReflectiveOperationException {
        if (directive.hasSubdirectives()) {
            throw mappingError(field, "expects key/value arguments without subdirectives");
        }

        final Class<?> keyType = resolveTypeArgument(field, 0);
        final Class<?> valueType = resolveTypeArgument(field, 1);
        if (isNotSupportedMapType(keyType) || isNotSupportedMapType(valueType)) {
            throw mappingError(field, "supports only Map<String, String>");
        }

        int valueCount = directive.valueCount();
        if (valueCount < 1 || valueCount > 2) {
            throw mappingError(field, "expects 'name key [value]'");
        }

        final Map<String, String> map = getOrCreateContainer(field, target, HashMap::new);
        map.put(directive.value(0), valueCount == 2 ? directive.value(1) : "");
    }

    @SuppressWarnings("unchecked")
    private static <T> T getOrCreateContainer(
            final Field field,
            final Object target,
            final Supplier<? extends T> defaultFactory
    ) throws ReflectiveOperationException {
        T value = (T) field.get(target);
        if (value != null) {
            return value;
        }

        final Class<?> fieldType = field.getType();
        if (fieldType.isInterface() || Modifier.isAbstract(fieldType.getModifiers())) {
            value = defaultFactory.get();
        } else {
            value = (T) fieldType.getDeclaredConstructor().newInstance();
        }

        field.set(target, value);
        return value;
    }

    private static Class<?> resolveTypeArgument(final Field field, final int index) {
        final Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return Object.class;
        }

        final Type argument = ((ParameterizedType) genericType).getActualTypeArguments()[index];
        if (argument instanceof Class<?>) {
            return (Class<?>) argument;
        }

        if (argument instanceof ParameterizedType) {
            final Type rawType = ((ParameterizedType) argument).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }

        return Object.class;
    }

    private static void validateNestedDirective(final Field field, final Directive directive) {
        if (directive.arguments().size() != 1) {
            throw mappingError(field, "expects only subdirectives and no scalar value arguments");
        }
    }

    private static String readSingleValue(final Field field, final Directive directive) {
        if (directive.valueCount() != 1) {
            throw mappingError(field, "expects exactly one value argument");
        }

        return directive.value(0);
    }

    private static ConfettiMappingException mappingError(Field field, String message) {
        return new ConfettiMappingException("Field '" + field.getName() + "' " + message);
    }

    private static Map<String, Field> buildFieldMap(final Class<?> clazz) {
        final Map<String, Field> map = new HashMap<>();
        for (Class<?> type = clazz; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                final ConfettiName annotation = field.getAnnotation(ConfettiName.class);
                final String name = annotation != null ? annotation.value() : field.getName();
                map.putIfAbsent(name, field);
            }
        }

        return map;
    }

    private static Object convert(final String value, final Class<?> type) {
        try {
            if (type == String.class || type == CharSequence.class || type == Object.class) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            }
            if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            }
            if (type == boolean.class || type == Boolean.class) {
                return parseBoolean(value);
            }
        } catch (final NumberFormatException e) {
            throw new ConfettiMappingException("Failed to convert '" + value + "' to " + type.getSimpleName(), e);
        }

        throw new ConfettiMappingException("Unsupported field type: " + type.getName());
    }

    private static boolean parseBoolean(final String value) {
        final String normalized = value.toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "true":
            case "yes":
            case "on":
            case "1":
                return true;
            case "false":
            case "no":
            case "off":
            case "0":
                return false;
            default:
                throw new ConfettiMappingException("Invalid boolean value: " + value);
        }
    }

    private static boolean isScalar(final Class<?> type) {
        return type == String.class || type == CharSequence.class || type == Object.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class;
    }

    private static boolean isNotSupportedMapType(final Class<?> type) {
        return type != String.class && type != Object.class;
    }

}

package com.valsong.swagger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.valsong.swagger.constant.SwaggerScannerConstants.COLLECTION_FIRST_INDEX;
import static com.valsong.swagger.constant.SwaggerScannerConstants.ONE_ELEMENT_COLLECTION_LENGTH;


/**
 * 反射工具类
 *
 * @author Val Song
 */
public class SwaggerReflectUtils {

    private static final Gson SERIALIZE_NULLS_GSON = new GsonBuilder().serializeNulls()
            .registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue()) {
                        return new JsonPrimitive(src.longValue());
                    }
                    return new JsonPrimitive(src);
                }
            })
            .setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private SwaggerReflectUtils() {
    }

    /**
     * 根据type和method获取所有泛型class
     *
     * @param type
     * @return
     */
    public static Map<Class<?>, Type> getGenericTypeMap(Type type) {
        Map<Class<?>, Type> genericTypeMap = new HashMap<>();

        Type genericSuperclass;
        if (type instanceof ParameterizedType) {
            Class<?> actualClazz = (Class<?>) ((ParameterizedType) type).getRawType();
            genericTypeMap.put(actualClazz, type);

            genericSuperclass = actualClazz.getGenericSuperclass();

        } else {
            genericSuperclass = ((Class<?>) type).getGenericSuperclass();
        }

        Class<?> actualSuperClazz;
        while (true) {
            if (genericSuperclass instanceof ParameterizedType) {
                actualSuperClazz = (Class<?>) ((ParameterizedType) genericSuperclass).getRawType();
            } else {
                actualSuperClazz = (Class<?>) genericSuperclass;
            }

            if (actualSuperClazz == null || Object.class.equals(actualSuperClazz)) {
                break;
            }
            genericTypeMap.put(actualSuperClazz, genericSuperclass);

            genericSuperclass = actualSuperClazz.getGenericSuperclass();
        }
        return genericTypeMap;
    }

    /**
     * 转换成JsonElement并替换所有JsonNull值
     *
     * @param obj
     * @return
     */
    public static JsonElement convertToJsonElementWithoutNull(Object obj) {
        JsonElement jsonElement = SERIALIZE_NULLS_GSON.toJsonTree(obj);
        if (jsonElement.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                if (entry.getValue() instanceof JsonNull) {
                    entry.setValue(new JsonObject());
                }
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                if (jsonArray.get(i) instanceof JsonNull) {
                    jsonArray.set(i, new JsonObject());
                }
            }
        }
        return jsonElement;
    }


    /**
     * 获取真正的类型
     *
     * @param type
     * @param genericTypeMap
     * @return
     */
    public static Type getActualTypeByTypeVariable(TypeVariable type, Map<Class<?>, Type> genericTypeMap) {

        final String typeName = type.getName();

        Class<?> genericDeclarationClazz = (Class<?>) type.getGenericDeclaration();

        TypeVariable<?>[] typeVariables = genericDeclarationClazz.getTypeParameters();

        Type genericType = genericTypeMap.get(genericDeclarationClazz);

        final int variableNum = typeVariables.length;

        int index = 0;

        for (int i = 0; i < variableNum; i++) {
            if (typeName.equals(typeVariables[i].getName())) {
                index = i;
                break;
            }
        }
        final Type actualType;
        if (genericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            actualType = actualTypeArguments[index];
        } else {
            actualType = Object.class;
        }
        return actualType;
    }

    /**
     * 获取真正的类型
     *
     * @param type
     * @return
     */
    public static Type getActualTypeByWildcardType(WildcardType type) {
        Type actualType;
        Type[] lowerBounds = type.getLowerBounds();
        Type[] upperBounds = type.getUpperBounds();
        if (lowerBounds != null && lowerBounds.length == ONE_ELEMENT_COLLECTION_LENGTH) {
            actualType = lowerBounds[COLLECTION_FIRST_INDEX];
        } else if (upperBounds != null && upperBounds.length == ONE_ELEMENT_COLLECTION_LENGTH) {
            actualType = upperBounds[COLLECTION_FIRST_INDEX];
        } else {
            actualType = Object.class;
        }
        return actualType;
    }

    /**
     * 获取真正的类型
     *
     * @param type
     * @param genericTypeMap
     * @return
     */
    public static Type getActualType(Type type, Map<Class<?>, Type> genericTypeMap) {
        Type actualType = null;
        if (type instanceof Class<?>) {

            if (((Class) type).isArray()) {
                actualType = ((Class) type).getComponentType();
            } else {
                actualType = type;
            }
        } else if (type instanceof ParameterizedType) {
            actualType = ((ParameterizedType) type).getRawType();
        } else if (type instanceof TypeVariable) {
            actualType = SwaggerReflectUtils.getActualTypeByTypeVariable((TypeVariable) type, genericTypeMap);
        } else if (type instanceof WildcardType) {
            actualType = SwaggerReflectUtils.getActualTypeByWildcardType((WildcardType) type);
        } else if (type instanceof GenericArrayType) {
            Type genericComponentType = ((GenericArrayType) type).getGenericComponentType();
            actualType = getActualType(genericComponentType, genericTypeMap);
            return Array.newInstance((Class<?>) actualType, ONE_ELEMENT_COLLECTION_LENGTH).getClass();
        }
        if (actualType instanceof Class<?>) {
            return actualType;
        } else {
            return getActualType(actualType, genericTypeMap);
        }
    }

    /**
     * 根据名称获取field
     *
     * @param clazz
     * @param fieldName
     * @return
     */
    public static Field findDeclaredField(Class<?> clazz, String fieldName) {
        Field[] currentClazzFields = clazz.getDeclaredFields();
        for (Field f : currentClazzFields) {
            if (fieldName.equals(f.getName())) {
                return f;
            }
        }
        Class<?> superClazz = clazz.getSuperclass();
        if (!Object.class.equals(superClazz)) {
            return findDeclaredField(superClazz, fieldName);
        }
        return null;
    }

    /**
     * 循环处理clazz的field
     *
     * @param fieldType
     * @param genericTypeMap
     * @return
     */
    public static List<Type> findAllTypeFromType(Type fieldType, Map<Class<?>, Type> genericTypeMap) {
        List<Type> allType = new ArrayList<>();
        if (fieldType instanceof ParameterizedType) {
            Type actualType = ((ParameterizedType) fieldType).getRawType();
            allType.add(actualType);
            Type[] types = ((ParameterizedType) fieldType).getActualTypeArguments();
            for (Type t : types) {
                allType.addAll(findAllTypeFromType(t, genericTypeMap));
            }
        } else if (fieldType instanceof TypeVariable) {
            Type actualType = SwaggerReflectUtils.getActualTypeByTypeVariable((TypeVariable) fieldType,
                    genericTypeMap);
            allType.addAll(findAllTypeFromType(actualType, genericTypeMap));
        } else if (fieldType instanceof WildcardType) {
            Type actualType = SwaggerReflectUtils.getActualTypeByWildcardType((WildcardType) fieldType);
            allType.addAll(findAllTypeFromType(actualType, genericTypeMap));
        } else if (fieldType instanceof GenericArrayType) {
            Type genericComponentType = ((GenericArrayType) fieldType).getGenericComponentType();
            allType.addAll(findAllTypeFromType(genericComponentType, genericTypeMap));
        } else if ((fieldType instanceof Class<?>)) {
            if (((Class) fieldType).isArray()) {
                Type actualType = ((Class) fieldType).getComponentType();
                //防止多重数组嵌套
                allType.addAll(findAllTypeFromType(actualType, genericTypeMap));
            } else {
                allType.add(fieldType);
            }
        }
        return allType;
    }


}

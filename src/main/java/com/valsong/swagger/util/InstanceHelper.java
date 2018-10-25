package com.valsong.swagger.util;


import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.reflect.TypeToken;
import com.valsong.swagger.exception.SwaggerScannerException;

import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * 无需构造器直接根据class创建实例
 *
 * @author Val Song
 */
public final class InstanceHelper {

    private InstanceHelper() {
    }

    private static ThreadLocal<ConstructorConstructor> CONSTRUCTOR_CONSTRUCTOR_HOLDER =
            ThreadLocal.withInitial(() -> new ConstructorConstructor(new HashMap<>(0)));

    /**
     * 不需要构造器创建实例,底层通过Unsafe实现
     *
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T newInstance(Type type) {
        TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(type);
        ObjectConstructor<T> objectConstructor = CONSTRUCTOR_CONSTRUCTOR_HOLDER.get().get(typeToken);
        try {
            return objectConstructor.construct();
        } catch (Exception e) {
            throw new SwaggerScannerException(" type: " + type + "  " + e.getMessage(), e);
        }

    }

}

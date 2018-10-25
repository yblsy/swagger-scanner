package com.valsong.swagger.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.valsong.swagger.exception.SwaggerScannerException;
import com.valsong.swagger.model.MethodProperty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

/**
 * 利用反射执行方法
 *
 * @author Val Song
 */

public class SwaggerScannerInvoker {

    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private static SwaggerApiRegistry swaggerApiRegistry;

    public SwaggerScannerInvoker(SwaggerApiRegistry swaggerApiRegistry) {
        SwaggerScannerInvoker.swaggerApiRegistry = swaggerApiRegistry;
    }

    /**
     * 根据方法名称和josn调用
     *
     * @param methodName
     * @param json
     * @return
     */
    public static Object invoke(String methodName, String json) {

        MethodProperty methodProperty = swaggerApiRegistry.getTriggerMethod(methodName);

        Method method = methodProperty.getMethod();

        Class<?> compositeParameterClazz = methodProperty.getCompositeParameterClazz();

        method.setAccessible(true);

        Object instance = methodProperty.getInstance();

        int parameterCount = method.getParameterCount();

        Type[] parameterTypes = method.getGenericParameterTypes();

        Object compositeParameter = GSON.fromJson(json, compositeParameterClazz);

        //将参数顺序调整至正确
        JsonObject parameterJsonObj = GSON.toJsonTree(compositeParameter).getAsJsonObject();

        Object[] params = new Object[parameterCount];

        int i = 0;
        for (Iterator<Map.Entry<String, JsonElement>> iter = parameterJsonObj.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, JsonElement> entry = iter.next();
            params[i] = GSON.fromJson(entry.getValue(), parameterTypes[i]);
            i++;
        }

        try {
            Object returnVal = method.invoke(instance, params);
            return returnVal;
        } catch (IllegalAccessException e) {
            throw new SwaggerScannerException("swagger invoke failed!", e);
        } catch (InvocationTargetException e) {
            throw new SwaggerScannerException("swagger invoke failed!", e);
        }

    }

}

package com.valsong.swagger.support;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 获取Swagger Api的描述
 *
 * @author Val Song
 * @date 2018/10/18
 * @since 1.0.0
 */
public class SwaggerApiDescHelper {

    /**
     * 获取参数描述
     *
     * @param method
     * @return
     */
    public static String parameterDesc(Method method) {
        StringBuilder parameterDescBuilder = new StringBuilder();
        Arrays.asList(method.getGenericParameterTypes())
                .stream()
                .map(Type::getTypeName)
                .collect(Collectors.toList())
                .forEach(typeStr -> parameterDescBuilder.append(typeStr).append(System.lineSeparator()));
        String parameterDesc = parameterDescBuilder.toString()
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return parameterDesc;
    }

    /**
     * 获取返回值描述
     *
     * @param method
     * @return
     */
    public static String returnDesc(Method method) {
        String returnDesc = method.getGenericReturnType().getTypeName();
        returnDesc = returnDesc.replace("<", "&lt;").replace(">", "&gt;");
        return returnDesc;
    }

}

package com.valsong.swagger.support;

import com.valsong.swagger.model.MethodProperty;
import com.valsong.swagger.model.SwaggerApiProperty;
import springfox.documentation.schema.Model;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.Tag;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SimpleSwaggerRegistry
 *
 * @author Val Song
 * @date 2018/10/18
 * @since 1.0.0
 */
public class SimpleSwaggerApiRegistry implements SwaggerApiRegistry {

    /**
     * SWAGGER_PROPERTIES
     */
    private final List<SwaggerApiProperty> swaggerApiProperties = new ArrayList<>();


    /**
     * swagger页面所需要的参数ApiDescription
     */
    private final List<ApiDescription> apiDescriptions = new ArrayList<>();


    /**
     * swagger页面所需要的参数Model
     */
    private final Map<String, Model> models = new HashMap<>();


    /**
     * swagger页面所需要的参数Model
     */
    private final Set<Tag> tags = new HashSet<>();

    /**
     * 通过页面触发的方法
     */
    public static final Map<String, MethodProperty> triggerMethods = new HashMap<>();


    @Override
    public String allocateMethodName(String beanName, Method method) {
        String beanMethodName = beanName + "." + method.getName();
        final String methodName = method.getName();
        List<Method> sameNameMethods =
                Arrays.asList(method.getDeclaringClass().getMethods())
                        .stream()
                        .filter(m -> methodName.equals(m.getName()))
                        .collect(Collectors.toList());

        for (int i = 0; i < sameNameMethods.size(); i++) {
            if (method.equals(sameNameMethods.get(i))) {
                if (i != 0) {
                    int index = i + 1;
                    beanMethodName = beanMethodName + "(" + index + ")";
                }
                break;
            }
        }
        return beanMethodName;
    }

    @Override
    public void registerSwaggerApiProperty(SwaggerApiProperty swaggerProperty) {
        swaggerApiProperties.add(swaggerProperty);
    }

    @Override
    public List<SwaggerApiProperty> getAllSwaggerApiProperties() {
        return swaggerApiProperties;
    }

    @Override
    public void registerApiDescription(ApiDescription apiDescription) {
        apiDescriptions.add(apiDescription);
    }

    @Override
    public List<ApiDescription> getAllApiDescriptions() {
        return apiDescriptions;
    }

    @Override
    public void registerModels(Map<String, Model> modelMap) {
        models.putAll(modelMap);
    }

    @Override
    public Map<String, Model> getModels() {
        return models;
    }

    @Override
    public void registerTriggerMethod(String methodName, MethodProperty methodProperty) {
        triggerMethods.put(methodName, methodProperty);
    }


    @Override
    public MethodProperty getTriggerMethod(String methodName) {
        return triggerMethods.get(methodName);
    }

    @Override
    public void registerTag(Tag tag) {
        tags.add(tag);
    }

    @Override
    public void registerTag(Set<Tag> tags) {
        tags.addAll(tags);
    }

    @Override
    public Set<Tag> getAllTags() {
        return tags;
    }

}

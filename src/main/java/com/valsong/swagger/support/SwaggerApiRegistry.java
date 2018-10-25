package com.valsong.swagger.support;

import com.valsong.swagger.model.MethodProperty;
import com.valsong.swagger.model.SwaggerApiProperty;
import springfox.documentation.schema.Model;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.Tag;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 存放swagger将扫描的内容展示到页面需要的内容
 *
 * @author: Val Song
 */
public interface SwaggerApiRegistry {

    /**
     *  申请方法名，防止重名
     *
     * @param beanName
     * @param method
     * @return
     */
    String allocateMethodName(String beanName, Method method);

    /**
     * 注册swaggerApiProperty
     *
     * @param swaggerApiProperty
     */
    void registerSwaggerApiProperty(SwaggerApiProperty swaggerApiProperty);

    /**
     * 获取所有SwaggerApiProperty
     *
     * @return
     */
    List<SwaggerApiProperty> getAllSwaggerApiProperties();


    /**
     * 注册ApiDescription
     *
     * @param apiDescription
     */
    void registerApiDescription(ApiDescription apiDescription);

    /**
     * 获取所有ApiDescription
     *
     * @return
     */
    List<ApiDescription> getAllApiDescriptions();

    /**
     * 注册Model
     *
     * @param modelMap
     */
    void registerModels(Map<String, Model> modelMap);

    /**
     * 获取所有ApiDescription
     *
     * @return
     */
    Map<String, Model> getModels();

    /**
     * 注册页面触发的方法
     *
     * @param methodName
     * @param methodProperty
     */
    void registerTriggerMethod(String methodName, MethodProperty methodProperty);

    /**
     * 获取需要调用的方法
     *
     * @param methodName
     * @return
     */
    MethodProperty getTriggerMethod(String methodName);

    /**
     * 注册tag
     *
     * @param tag
     */
    void registerTag(Tag tag);

    /**
     * 注册tags
     *
     * @param tags
     */
    void registerTag(Set<Tag> tags);

    /**
     * 获取所有tags
     *
     * @return
     */
    Set<Tag> getAllTags();


}

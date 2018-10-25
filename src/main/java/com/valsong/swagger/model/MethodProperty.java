package com.valsong.swagger.model;

import java.lang.reflect.Method;

/**
 * MethodProperty
 *
 * @author Val Song
 */
public class MethodProperty {


    /**
     * bean的名称
     */
    private String beanName;

    /**
     * 实例
     */
    private Object instance;

    /**
     * 方法
     */
    private Method method;


    /**
     * 使用byte buddy根据方法参数作为field生成的class
     */
    private Class<?> compositeParameterClazz;


    public MethodProperty() {
    }

    public MethodProperty(String beanName, Object instance, Method method, Class<?> compositeParameterClazz) {
        this.beanName = beanName;
        this.instance = instance;
        this.method = method;
        this.compositeParameterClazz = compositeParameterClazz;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getCompositeParameterClazz() {
        return compositeParameterClazz;
    }

    public void setCompositeParameterClazz(Class<?> compositeParameterClazz) {
        this.compositeParameterClazz = compositeParameterClazz;
    }

    @Override
    public String toString() {
        return "MethodProperty{" +
                "beanName='" + beanName + '\'' +
                ", instance=" + instance +
                ", method=" + method +
                ", compositeParameterClazz=" + compositeParameterClazz +
                '}';
    }
}

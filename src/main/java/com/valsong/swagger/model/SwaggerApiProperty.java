package com.valsong.swagger.model;

import io.swagger.annotations.ApiOperation;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * 存放Swagger需要展示的资源的属性
 *
 * @author: Val Song
 */
public class SwaggerApiProperty {

    /**
     * swagger的tag
     */
    private String tag;

    /**
     * bean名称.method名称
     */
    private String name;

    /**
     * bean的class
     */
    private Class<?> beanClazz;

    /**
     * 方法
     */
    private Method method;

    /**
     * 方法参数类型
     */
    private Type[] genericParameterTypes;

    /**
     * 返回值类型
     */
    private Type genericReturnType;


    /**
     * byte buddy将多个参数混合生成的class
     */
    private Class<?> compositeParameterClazz;

    /**
     * @ApiOperation 注解，支持summary和notes
     */
    private ApiOperation apiOperation;

    /**
     * 参数描述  注意此处的parameterDesc中需要将"<"替换成"&lt;"、">"替换成"&gt;"
     */
    private String parameterDesc;

    /**
     * 返回值描述 注意此处的returnDesc中需要将"<"替换成"&lt;"、">"替换成"&gt;"
     */
    private String returnDesc;

    public SwaggerApiProperty() {
    }

    private SwaggerApiProperty(Builder builder) {
        setTag(builder.tag);
        setName(builder.name);
        setBeanClazz(builder.beanClazz);
        setMethod(builder.method);
        setGenericParameterTypes(builder.genericParameterTypes);
        setGenericReturnType(builder.genericReturnType);
        setCompositeParameterClazz(builder.compositeParameterClazz);
        setApiOperation(builder.apiOperation);
        setParameterDesc(builder.parameterDesc);
        setReturnDesc(builder.returnDesc);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getBeanClazz() {
        return beanClazz;
    }

    public void setBeanClazz(Class<?> beanClazz) {
        this.beanClazz = beanClazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Type[] getGenericParameterTypes() {
        return genericParameterTypes;
    }

    public void setGenericParameterTypes(Type[] genericParameterTypes) {
        this.genericParameterTypes = genericParameterTypes;
    }

    public Type getGenericReturnType() {
        return genericReturnType;
    }

    public void setGenericReturnType(Type genericReturnType) {
        this.genericReturnType = genericReturnType;
    }

    public Class<?> getCompositeParameterClazz() {
        return compositeParameterClazz;
    }

    public void setCompositeParameterClazz(Class<?> compositeParameterClazz) {
        this.compositeParameterClazz = compositeParameterClazz;
    }

    public ApiOperation getApiOperation() {
        return apiOperation;
    }

    public void setApiOperation(ApiOperation apiOperation) {
        this.apiOperation = apiOperation;
    }

    public String getParameterDesc() {
        return parameterDesc;
    }

    public void setParameterDesc(String parameterDesc) {
        this.parameterDesc = parameterDesc;
    }

    public String getReturnDesc() {
        return returnDesc;
    }

    public void setReturnDesc(String returnDesc) {
        this.returnDesc = returnDesc;
    }





    public static final class Builder {
        private String tag;
        private String name;
        private Class<?> beanClazz;
        private Method method;
        private Type[] genericParameterTypes;
        private Type genericReturnType;
        private Class<?> compositeParameterClazz;
        private ApiOperation apiOperation;
        private String parameterDesc;
        private String returnDesc;

        private Builder() {
        }

        public Builder tag(String val) {
            tag = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder beanClazz(Class<?> val) {
            beanClazz = val;
            return this;
        }

        public Builder method(Method val) {
            method = val;
            return this;
        }

        public Builder genericParameterTypes(Type[] val) {
            genericParameterTypes = val;
            return this;
        }

        public Builder genericReturnType(Type val) {
            genericReturnType = val;
            return this;
        }

        public Builder compositeParameterClazz(Class<?> val) {
            compositeParameterClazz = val;
            return this;
        }

        public Builder apiOperation(ApiOperation val) {
            apiOperation = val;
            return this;
        }

        public Builder parameterDesc(String val) {
            parameterDesc = val;
            return this;
        }

        public Builder returnDesc(String val) {
            returnDesc = val;
            return this;
        }

        public SwaggerApiProperty build() {
            return new SwaggerApiProperty(this);
        }
    }

    @Override
    public String toString() {
        return "SwaggerApiProperty{" +
                "tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", beanClazz=" + beanClazz +
                ", method=" + method +
                ", genericParameterTypes=" + Arrays.toString(genericParameterTypes) +
                ", genericReturnType=" + genericReturnType +
                ", compositeParameterClazz=" + compositeParameterClazz +
                ", apiOperation=" + apiOperation +
                ", parameterDesc='" + parameterDesc + '\'' +
                ", returnDesc='" + returnDesc + '\'' +
                '}';
    }
}

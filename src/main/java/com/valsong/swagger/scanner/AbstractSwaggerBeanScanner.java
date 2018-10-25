package com.valsong.swagger.scanner;

import com.valsong.swagger.model.MethodProperty;
import com.valsong.swagger.model.SwaggerApiProperty;
import com.valsong.swagger.support.SwaggerApiDescHelper;
import com.valsong.swagger.support.SwaggerApiRegistry;
import com.valsong.swagger.util.SwaggerExampleClassBuilder;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.service.Tag;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;


/**
 * 扫描spring bean的标有指定注解的方法
 *
 * @author Val Song
 */
public abstract class AbstractSwaggerBeanScanner<MA extends Annotation> implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSwaggerBeanScanner.class);

    private SwaggerApiRegistry swaggerApiRegistry;

    public AbstractSwaggerBeanScanner(SwaggerApiRegistry swaggerApiRegistry) {
        this.swaggerApiRegistry = swaggerApiRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 必须填写
     *
     * 需要扫描的写在Spring的bean的方法上的注解
     *
     * @return
     */
    public abstract Class<MA> methodAnnotationClazz();

    /**
     * 可以返回null
     *
     * 返回添加在swagger url上的后缀也可以做一些其他操作
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract String pathSuffix(MA methodAnnotation, MethodProperty methodProperty);


    /**
     * 必须填写
     *
     * 返回该方法需要的swagger的tag new Tag(name,desc)
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract Tag tag(MA methodAnnotation, MethodProperty methodProperty);

    /**
     * 如果返回false则不扫描该方法到swagger
     * 推荐返回true或者根据条件返回true
     *
     * 是否扫描该方法
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract boolean isAccept(MA methodAnnotation, MethodProperty methodProperty);


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {

            Class<?> clazz = bean.getClass();

            ApiIgnore apiIgnore = AnnotationUtils.findAnnotation(clazz, ApiIgnore.class);
            //是否被忽略
            if (apiIgnore != null) {
                logger.info(" clazz: {} with @ApiIgnore is skipped by AbstractSwaggerBeanScanner .", clazz);
                return bean;
            }

            final Class<MA> methodAnnotationClazz = methodAnnotationClazz();

            if (methodAnnotationClazz == null) {
                return bean;
            }

            scan(methodAnnotationClazz, beanName, bean);

        } catch (Exception e) {
            logger.error("An error occurred, when swagger scan bean " + bean.getClass(), e);
            throw new ExceptionInInitializerError(e);
        }
        return bean;
    }

    /**
     * 扫描
     *
     * @param methodAnnotationClazz
     * @param beanName
     * @param bean
     */
    private void scan(Class<MA> methodAnnotationClazz, String beanName, Object bean) {
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
        if (methods != null && methods.length > 0) {
            for (Method method : methods) {

                ApiIgnore methodApiIgnore = AnnotationUtils.findAnnotation(method, ApiIgnore.class);
                //是否被忽略
                if (methodApiIgnore != null) {
                    logger.info(" method: {} with @ApiIgnore is skipped by AbstractSwaggerBeanScanner .", method);
                    continue;
                }

                MA methodAnnotation = AnnotationUtils.findAnnotation(method, methodAnnotationClazz);
                if (methodAnnotation == null) {
                    continue;
                }

                Class<?> compositeParameterClazz = SwaggerExampleClassBuilder.build(method, bean.getClass());

                MethodProperty methodProperty = new MethodProperty(beanName, bean, method, compositeParameterClazz);

                boolean isAccept = isAccept(methodAnnotation, methodProperty);

                if (!isAccept) {
                    continue;
                }

                String pathSuffix = pathSuffix(methodAnnotation, methodProperty);

                Tag tag = tag(methodAnnotation, methodProperty);

                swaggerApiRegistry.registerTag(tag);

                //申请方法名，防止重名
                String doMethodName = swaggerApiRegistry.allocateMethodName(beanName, method);

                String methodToShow;
                if (StringUtils.isNotBlank(pathSuffix)) {
                    methodToShow = doMethodName + "#" + pathSuffix;
                } else {
                    methodToShow = doMethodName;
                }

                swaggerApiRegistry.registerTriggerMethod(doMethodName, methodProperty);

                Type returnType = method.getGenericReturnType();

                String parameterDesc = SwaggerApiDescHelper.parameterDesc(method);

                String returnDesc = SwaggerApiDescHelper.returnDesc(method);

                //暂时只支持 value 、 notes 两个属性
                ApiOperation apiOperation = AnnotationUtils.findAnnotation(method, ApiOperation.class);
                swaggerApiRegistry.registerSwaggerApiProperty(SwaggerApiProperty.newBuilder()
                        .tag(tag.getName())
                        .name(methodToShow)
                        .beanClazz(bean.getClass())
                        .method(method)
                        .genericParameterTypes(method.getGenericParameterTypes())
                        .compositeParameterClazz(compositeParameterClazz)
                        .genericReturnType(returnType)
                        .apiOperation(apiOperation)
                        .parameterDesc(parameterDesc)
                        .returnDesc(returnDesc)
                        .build());

            }
        }
    }


}
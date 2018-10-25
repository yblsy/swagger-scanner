package com.valsong.swagger.scanner;

import com.valsong.swagger.model.MethodProperty;
import com.valsong.swagger.model.SwaggerApiProperty;
import com.valsong.swagger.support.SwaggerApiDescHelper;
import com.valsong.swagger.support.SwaggerApiRegistry;
import com.valsong.swagger.util.SwaggerExampleClassBuilder;
import io.swagger.annotations.ApiOperation;
import javassist.util.proxy.ProxyObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.service.Tag;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;


/**
 * 扫描手动注入到beanFactory的接口的方法
 *
 * @author: Val Song
 */
public abstract class AbstractSwaggerInterfaceScanner<TA extends Annotation, MA extends Annotation> implements BeanPostProcessor,
        ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSwaggerInterfaceScanner.class);

    private SwaggerApiRegistry swaggerApiRegistry;

    public AbstractSwaggerInterfaceScanner(SwaggerApiRegistry swaggerApiRegistry) {
        this.swaggerApiRegistry = swaggerApiRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 需要扫描的写在接口上的注解
     *
     * @return
     */
    public abstract Class<TA> typeAnnotationClazz();


    /**
     * 需要扫描的写在接口方法上的注解
     *
     * @return
     */
    public abstract Class<MA> methodAnnotationClazz();

    /**
     * 返回添加在swagger url上的后缀也可以做一些其他操作
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract String pathSuffix(MA methodAnnotation, MethodProperty methodProperty);


    /**
     * 返回该方法需要的swagger的tag new Tag(name,desc)
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract Tag tag(MA methodAnnotation, MethodProperty methodProperty);

    /**
     * 是否扫描该方法
     *
     * @param methodAnnotation
     * @param methodProperty
     * @return
     */
    public abstract boolean isAccept(MA methodAnnotation, MethodProperty methodProperty);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        final Class<TA> typeAnnotationClazz = typeAnnotationClazz();

        Map<String, Object> needScanBeans = applicationContext.getBeansWithAnnotation(typeAnnotationClazz);

        needScanBeans.forEach((beanName, bean) -> {

            scan(typeAnnotationClazz, beanName, bean);

        });
    }

    /**
     * 扫描
     *
     * @param typeAnnotationClazz
     * @param beanName
     * @param bean
     */
    private void scan(Class<TA> typeAnnotationClazz, String beanName, Object bean) {
        Class<?> beanClazz;
        //对于使用javassist生成的bean使用接口生成paramClass，防止泛型丢失
        if (bean instanceof ProxyObject) {
            beanClazz = Arrays.asList(bean.getClass()
                    .getGenericInterfaces())
                    .stream()
                    .map(Class.class::cast)
                    .filter(clazz -> clazz.getAnnotation(typeAnnotationClazz) != null)
                    .findFirst()
                    .orElse(bean.getClass());
        } else {
            beanClazz = bean.getClass();
        }

        ApiIgnore apiIgnore = AnnotationUtils.findAnnotation(beanClazz, ApiIgnore.class);
        //是否被忽略
        if (apiIgnore != null) {
            logger.info(" clazz: {} with @ApiIgnore is skipped by AbstractSwaggerInterfaceScanner .", beanClazz);
            return;
        }


        TA typeAnnotation = AnnotationUtils.findAnnotation(beanClazz, typeAnnotationClazz);

        if (typeAnnotation == null) {
            return;
        }

        final Class<MA> methodAnnotationClazz = methodAnnotationClazz();

        Method[] methods = ReflectionUtils.getAllDeclaredMethods(beanClazz);

        if (methods != null && methods.length > 0) {
            for (Method method : methods) {

                ApiIgnore methodApiIgnore = AnnotationUtils.findAnnotation(method, ApiIgnore.class);
                //是否被忽略
                if (methodApiIgnore != null) {
                    logger.info(" method: {} with @ApiIgnore is skipped by AbstractSwaggerInterfaceScanner .", method);
                    continue;
                }

                MA methodAnnotation = AnnotationUtils.findAnnotation(method, methodAnnotationClazz);
                if (methodAnnotation == null) {
                    continue;
                }

                Class<?> compositeParameterClazz = SwaggerExampleClassBuilder.build(method, beanClazz);

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

                //暂时只支持 value 、 notes 两个属性
                ApiOperation apiOperation = AnnotationUtils.findAnnotation(method, ApiOperation.class);

                String parameterDesc = SwaggerApiDescHelper.parameterDesc(method);

                String returnDesc = SwaggerApiDescHelper.returnDesc(method);

                swaggerApiRegistry.registerSwaggerApiProperty(SwaggerApiProperty.newBuilder()
                        .tag(tag.getName())
                        .name(methodToShow)
                        .beanClazz(beanClazz)
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
package com.valsong.swagger.configuration;

import com.fasterxml.classmate.TypeResolver;
import com.valsong.swagger.SwaggerScannerServlet;
import com.valsong.swagger.annotation.SwaggerScanner;
import com.valsong.swagger.support.SimpleSwaggerApiRegistry;
import com.valsong.swagger.support.SwaggerApiDescriptionProvider;
import com.valsong.swagger.support.SwaggerApiRegistry;
import com.valsong.swagger.support.SwaggerModelProvider;
import com.valsong.swagger.support.SwaggerScannerInvoker;
import com.valsong.swagger.support.SwaggerScannerPluginsBootstrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import springfox.documentation.schema.ModelProvider;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Map;

import static com.valsong.swagger.constant.SwaggerScannerConstants.PATH_PREFIX;
import static com.valsong.swagger.constant.SwaggerScannerConstants.URL_PATTERN;

/**
 * Swagger设置
 *
 * @author: Val Song
 */
public class SwaggerScannerConfiguration implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Docket的Bean的名称
     */
    private static String docketBeanName;

    @Bean
    public SwaggerScannerPluginsBootstrapper swaggerScannerPluginsBootstrapper(@Autowired SwaggerApiRegistry swaggerApiRegistry) {
        Docket docket = getDocket();
        return new SwaggerScannerPluginsBootstrapper(docket, swaggerApiRegistry);
    }

    @Bean
    public SwaggerApiRegistry swaggerApiRegistry() {
        return new SimpleSwaggerApiRegistry();
    }

    @Bean
    public ServletRegistrationBean swaggerScannerServlet() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        servletRegistrationBean.setServlet(new SwaggerScannerServlet());
        servletRegistrationBean.addUrlMappings(URL_PATTERN);
        return servletRegistrationBean;
    }

    @Bean
    public SwaggerApiDescriptionProvider swaggerApiDescriptionProvider(@Autowired SwaggerApiRegistry swaggerApiRegistry,
                                                                       @Autowired TypeResolver typeResolver,
                                                                       @Autowired TypeNameExtractor typeNameExtractor,
                                                                       @Autowired @Qualifier("default") ModelProvider modelProvider) {
        Docket docket = getDocket();
        return new SwaggerApiDescriptionProvider(swaggerApiRegistry, docket, typeResolver, typeNameExtractor,
                modelProvider);
    }

    @Bean
    public SwaggerModelProvider swaggerModelProvider(@Autowired SwaggerApiRegistry swaggerApiRegistry) {
        return new SwaggerModelProvider(swaggerApiRegistry);
    }


    @Bean
    public SwaggerScannerInvoker swaggerScannerInvoker(@Autowired SwaggerApiRegistry swaggerApiRegistry) {
        return new SwaggerScannerInvoker(swaggerApiRegistry);
    }


    /**
     * 获取Docket
     *
     * @return
     */
    private Docket getDocket() {
        Docket docket;
        //如果没有指定docketBeanName则根据类型来获取Bean
        if (StringUtils.isBlank(SwaggerScannerConfiguration.docketBeanName)) {
            docket = SwaggerScannerConfiguration.getBean(Docket.class);
        } else {
            docket = SwaggerScannerConfiguration.getBean(SwaggerScannerConfiguration.docketBeanName);
        }
        return docket;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SwaggerScannerConfiguration.applicationContext = applicationContext;
    }

    /**
     * 根据名称获取Bean
     *
     * @param beanName
     * @param <T>
     * @return
     */
    private static <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    /**
     * 根据Class获取Bean
     *
     * @param clazz
     * @param <T>
     * @return
     */
    private static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 用于获取@EnableEventDriven的docketBeanName
     */
    public static class SwaggerScannerRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(SwaggerScanner.class.getName());
            AnnotationAttributes enableAttributes = AnnotationAttributes.fromMap(annotationAttributes);
            boolean enable = enableAttributes.getBoolean("enable");

            //如果未开启则结束
            if (!enable) {
                return;
            }

            BeanDefinition eventDrivenSwaggerScannerConfigurationDefinition = BeanDefinitionBuilder.rootBeanDefinition
                    (SwaggerScannerConfiguration.class).getBeanDefinition();
            registry.registerBeanDefinition("swaggerScannerConfiguration",
                    eventDrivenSwaggerScannerConfigurationDefinition);
            String docketBeanName = enableAttributes.getString("docketBeanName");
            if (StringUtils.isNotBlank(docketBeanName)) {
                SwaggerScannerConfiguration.docketBeanName = docketBeanName;
            }

            String urlPattern = enableAttributes.getString("urlPattern");
            if (StringUtils.isNotBlank(urlPattern)) {
                URL_PATTERN = urlPattern;
                PATH_PREFIX = urlPattern + "?";
            }

        }

    }

}

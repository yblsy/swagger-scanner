/*
 *
 */
package com.valsong.swagger.annotation;

import com.valsong.swagger.configuration.SwaggerScannerConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用SwaggerScanner
 *
 * @author Val Song
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SwaggerScannerConfiguration.SwaggerScannerRegistrar.class})
public @interface SwaggerScanner {

    /**
     * 是否开启swagger扫描,目前仅支持swagger2.9.2及以上版本
     *
     * @return
     */
    boolean enable() default true;

    /**
     * 指定Swagger docket bean 的名称
     * 如果项目中有多个Docket则需要指定docketBeanName
     * 如果只有一个Docket则不用指定
     *
     * @return
     */
    String docketBeanName() default "";

    /**
     * swagger 的servlet拦截的url
     * @return
     */
    String urlPattern() default "/swagger_scanner";

}
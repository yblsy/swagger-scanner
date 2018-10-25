package com.valsong.swagger.support;

import org.springframework.context.SmartLifecycle;
import springfox.documentation.service.Tag;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Set;


/**
 * 扫描swagger后需要初始化的内容
 *
 * @author: Val Song
 */
public class SwaggerScannerPluginsBootstrapper implements SmartLifecycle {

    private SwaggerApiRegistry swaggerApiRegistry;

    private Docket docket;

    public SwaggerScannerPluginsBootstrapper(Docket docket, SwaggerApiRegistry swaggerApiRegistry) {
        this.docket = docket;
        this.swaggerApiRegistry = swaggerApiRegistry;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {

    }

    @Override
    public void start() {
        registerTags();
    }

    /**
     * 给指定的docket添加tags
     */
    private void registerTags() {
        Set<Tag> tags = swaggerApiRegistry.getAllTags();
        tags.forEach(docket::tags);
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return -1;
    }

}

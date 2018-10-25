package com.valsong.swagger.support;

import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;

/**
 * 手动添加Model到Swagger
 *
 * @author Val Song
 *
 */
public class SwaggerModelProvider implements ApiListingBuilderPlugin {

    private SwaggerApiRegistry swaggerApiRegistry;

    public SwaggerModelProvider(SwaggerApiRegistry swaggerApiRegistry) {
        this.swaggerApiRegistry = swaggerApiRegistry;
    }

    @Override
    public void apply(ApiListingContext apiListingContext) {
        if (!swaggerApiRegistry.getModels().isEmpty()) {
            apiListingContext.apiListingBuilder().models(swaggerApiRegistry.getModels());
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
package com.valsong.swagger.support;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.valsong.swagger.model.SwaggerApiProperty;
import com.valsong.swagger.util.FieldTypesDiscover;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.Model;
import springfox.documentation.schema.ModelProvider;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.ResolvedTypes;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.service.Operation;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.ApiListingScannerPlugin;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spi.service.contexts.OperationModelContextsBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.readers.operation.CachingOperationNameGenerator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.valsong.swagger.constant.SwaggerScannerConstants.ACCESS;
import static com.valsong.swagger.constant.SwaggerScannerConstants.BODY;
import static com.valsong.swagger.constant.SwaggerScannerConstants.CLOSE_PRE;
import static com.valsong.swagger.constant.SwaggerScannerConstants.OPEN_PRE;
import static com.valsong.swagger.constant.SwaggerScannerConstants.OPEN_PRE_FONT_NORMAL;
import static com.valsong.swagger.constant.SwaggerScannerConstants.PATH_PREFIX;


/**
 * 手动添加ApiDescriptions到Swagger
 *
 * @author: Val Song
 */
public class SwaggerApiDescriptionProvider implements ApiListingScannerPlugin {

    private Docket docket;

    private ModelProvider modelProvider;

    private TypeNameExtractor typeNameExtractor;

    private TypeResolver typeResolver;

    private SwaggerApiRegistry swaggerApiRegistry;

    public SwaggerApiDescriptionProvider(SwaggerApiRegistry swaggerApiRegistry,
                                         Docket docket,
                                         TypeResolver typeResolver,
                                         TypeNameExtractor typeNameExtractor, ModelProvider modelProvider) {
        this.swaggerApiRegistry = swaggerApiRegistry;
        this.docket = docket;
        this.typeResolver = typeResolver;
        this.typeNameExtractor = typeNameExtractor;
        this.modelProvider = modelProvider;
    }

    @Override
    public List<ApiDescription> apply(DocumentationContext documentationContext) {
        //只为指定的docket提供
        if (docket.getGroupName().equals(documentationContext.getGroupName())) {

            Set<Tag> tags = swaggerApiRegistry.getAllTags();
            //添加tags到docket
            tags.forEach(docket::tags);

            List<SwaggerApiProperty> swaggerApiProperties = swaggerApiRegistry.getAllSwaggerApiProperties();

            List<ApiDescription> apiDescriptions = new ArrayList<>();

            if (swaggerApiProperties != null && !swaggerApiProperties.isEmpty()) {

                for (SwaggerApiProperty swaggerApiProperty : swaggerApiProperties) {

                    //获取Models
                    Map<String, Model> models = resolveModels(documentationContext, swaggerApiProperty);

                    if (models != null && !models.isEmpty()) {
                        swaggerApiRegistry.registerModels(models);
                    }

                    //解析ApiDescription
                    ApiDescription apiDescription = resolveApiDescription(documentationContext, swaggerApiProperty);

                    apiDescriptions.add(apiDescription);

                }
            }
            return apiDescriptions;
        }
        return Collections.emptyList();
    }

    /**
     * 解析ApiDescription
     *
     * @param documentationContext
     * @param swaggerApiProperty
     * @return
     */
    private ApiDescription resolveApiDescription(DocumentationContext documentationContext,
                                                 SwaggerApiProperty swaggerApiProperty) {

        final String groupName = docket.getGroupName();

        String tag = swaggerApiProperty.getTag();

        String apiName = swaggerApiProperty.getName();

        ApiOperation apiOperation = swaggerApiProperty.getApiOperation();

        String summary = null;
        String notes = null;
        if (apiOperation != null) {
            summary = apiOperation.value();
            notes = apiOperation.notes();
        }

        //路径
        final String path = PATH_PREFIX + apiName;

        final List<Parameter> parameters = getParameters(documentationContext, swaggerApiProperty);

        final Set<ResponseMessage> responseMessages = getResponseMessages(documentationContext, swaggerApiProperty);

        //操作
        final List<Operation> operations = Lists.newArrayList(
                new OperationBuilder(
                        new CachingOperationNameGenerator())
                        .uniqueId(apiName)
                        //http请求类型
                        .method(HttpMethod.POST)
                        .consumes(Sets.newHashSet(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .produces(Sets.newHashSet(MediaType.APPLICATION_JSON_UTF8_VALUE))
                        .summary(summary)
                        .notes(notes)
                        .tags(Sets.newHashSet(tag))
                        .parameters(parameters)
                        .responseMessages(responseMessages)
                        .build()
        );

        return new ApiDescription(groupName, path, null, operations, false);
    }


    /**
     * 获取Swagger需要的参数
     *
     * @param documentationContext
     * @param swaggerApiProperty
     * @return
     */
    private List<Parameter> getParameters(DocumentationContext documentationContext,
                                          SwaggerApiProperty swaggerApiProperty) {


        Class<?> compositeParameterClazz = swaggerApiProperty.getCompositeParameterClazz();
        final ResolvedType parameterResolvedType = typeResolver.resolve(compositeParameterClazz);

        //  docket.additionalModels(parameterResolvedType);
        ModelContext parameterModelContext = ModelContext.returnValue(documentationContext.getGroupName(),
                compositeParameterClazz,
                documentationContext.getDocumentationType(),
                documentationContext.getAlternateTypeProvider(),
                documentationContext.getGenericsNamingStrategy(),
                documentationContext.getIgnorableParameterTypes());

        final ModelRef parameterModelRef = (ModelRef) ResolvedTypes.modelRefFactory(parameterModelContext,
                typeNameExtractor).apply(parameterResolvedType);

        //注意此处的parameterDesc中需要将"<"替换成"&lt;"、">"替换成"&gt;"
        String parameterDesc = swaggerApiProperty.getParameterDesc();

        final Parameter parameter = new ParameterBuilder()
                .name(BODY)
                .description(OPEN_PRE + parameterDesc + CLOSE_PRE)
                .type(parameterResolvedType)
                .modelRef(parameterModelRef)
                .parameterType(BODY)
                .parameterAccess(ACCESS)
                .required(true)
                .build();

        return Lists.newArrayList(parameter);
    }


    /**
     * 获取Swagger需要的返回值
     *
     * @param documentationContext
     * @param swaggerApiProperty
     * @return
     */
    private Set<ResponseMessage> getResponseMessages(DocumentationContext documentationContext,
                                                     SwaggerApiProperty swaggerApiProperty) {

        Type genericReturnType = swaggerApiProperty.getGenericReturnType();
        ResolvedType returnResolvedType = typeResolver.resolve(genericReturnType);

        // docket.additionalModels(returnResolvedType);
        ModelContext returnModelContext = ModelContext.returnValue(documentationContext.getGroupName(),
                genericReturnType,
                documentationContext.getDocumentationType(),
                documentationContext.getAlternateTypeProvider(),
                documentationContext.getGenericsNamingStrategy(),
                documentationContext.getIgnorableParameterTypes());

        ModelRef returnModelRef =
                (ModelRef) ResolvedTypes.modelRefFactory(returnModelContext, typeNameExtractor).apply(returnResolvedType);

        //注意此处的returnDesc中需要将"<"替换成"&lt;"、">"替换成"&gt;"
        String returnDesc = swaggerApiProperty.getReturnDesc();

        //结果集
        return Sets.newHashSet(
                new ResponseMessageBuilder().code(HttpStatus.OK.value())
                        .message(OPEN_PRE_FONT_NORMAL + returnDesc + CLOSE_PRE)
                        .responseModel(returnModelRef).build());
    }

    /**
     * 获取Models
     *
     * @param documentationContext
     * @param swaggerApiProperty
     * @return
     */
    private Map<String, Model> resolveModels(DocumentationContext documentationContext,
                                             SwaggerApiProperty swaggerApiProperty) {
        final OperationModelContextsBuilder builder = new OperationModelContextsBuilder(
                documentationContext.getGroupName(),
                documentationContext.getDocumentationType(),
                documentationContext.getAlternateTypeProvider(),
                documentationContext.getGenericsNamingStrategy(),
                documentationContext.getIgnorableParameterTypes());

        builder.addInputParam(swaggerApiProperty.getCompositeParameterClazz());

        Class<?> beanClazz = swaggerApiProperty.getBeanClazz();

        Type[] genericParameterTypes = swaggerApiProperty.getGenericParameterTypes();

        Arrays.asList(genericParameterTypes).forEach(type -> {
                    builder.addInputParam(type);
                    Set<Type> paramFieldTypes = FieldTypesDiscover.getFieldTypes(type, beanClazz);
                    paramFieldTypes.forEach(builder::addInputParam);
                }
        );


        Type genericReturnType = swaggerApiProperty.getGenericReturnType();
        //如果返回值不是void类型，一定要注意否则swagger-ui.html会报错
        if (!void.class.equals(genericReturnType)) {
            builder.addReturn(genericReturnType);
            Set<Type> returnFieldTypes = FieldTypesDiscover.getFieldTypes(genericReturnType, beanClazz);
            returnFieldTypes.forEach(builder::addInputParam);
        }

        Set<ModelContext> modelContexts = builder.build();

        if (modelContexts != null && !modelContexts.isEmpty()) {
            Map<String, Model> models = modelContexts.stream()
                    .map(modelProvider::modelFor)
                    .filter(Optional::isPresent)
                    .collect(Collectors.toMap(
                            o -> o.get().getName(),
                            Optional::get,
                            (a, b) -> a,
                            HashMap::new));
            return models;
        }

        return Collections.emptyMap();


    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return DocumentationType.SWAGGER_2.equals(documentationType);
    }

}
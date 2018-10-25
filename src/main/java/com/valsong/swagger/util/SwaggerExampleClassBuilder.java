package com.valsong.swagger.util;

import com.valsong.swagger.model.SwaggerExample;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 生成Swagger页面example所需要的class
 *
 * @author: Val Song
 */
public class SwaggerExampleClassBuilder {

    /**
     * 没有RawType
     */
    private static final Integer NON_RAW_TYPE = -1;


    /**
     * 用于获取方法的参数名称
     */
    private static final ThreadLocal<LocalVariableTableParameterNameDiscoverer> PARAMETER_NAME_DISCOVERER =
            ThreadLocal.withInitial(() -> new LocalVariableTableParameterNameDiscoverer());

    /**
     * 根据方法参数生成对应的class
     *
     * @param method 方法
     * @param clazz  调用方法的bean的class
     * @return
     */
    public static Class<?> build(Method method, Class<?> clazz) {

        //方法不能为空
        Objects.requireNonNull(method, "method can't be null!");

        //方法对应的参数名称
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.get().getParameterNames(method);

        //如果参数个数为0
        if (parameterNames == null || parameterNames.length == 0) {
            parameterNames = method.getParameters().length == 0 ? new String[0] :
                    Arrays.asList(method.getParameters()).stream().map(Parameter::getName).collect(Collectors.toList()).toArray(new String[0]);
        }

        //如果参数个数为0,则直接返回SwaggerExample
        if (parameterNames == null || parameterNames.length == 0) {
            return SwaggerExample.class;
        }

        //方法参数的泛型
        Type[] parameterTypes = method.getGenericParameterTypes();

        int size = parameterTypes.length;

        TypeProperty[] typeProperties = new TypeProperty[size];

        for (int i = 0; i < size; i++) {
            typeProperties[i] = new TypeProperty(parameterTypes[i], false);
        }

        Map<Class<?>, Type> genericTypeMap = SwaggerReflectUtils.getGenericTypeMap(clazz);

        //使用递归将方法参数类型排序
        Map<Integer, ParameterType> parameterTypeMap = processParameterTypes(typeProperties, null, null,
                genericTypeMap);

        // 构建Class之前准备好所有需要的泛型
        Map<Integer, TypeDefinition> preparedGeneric = preparedGenericBeforeBuildClass(parameterTypeMap);

        //构建Class
        Class<?> dynamicClass = buildClass(parameterNames, preparedGeneric);

        return dynamicClass;

    }

    /**
     * 根据方法参数生成对应的class
     *
     * @param parameterTypes 方法参数
     * @return
     */
    public static Class<?> build(Type[] parameterTypes) {

        int parameterNum = parameterTypes.length;

        TypeProperty[] typeProperties = new TypeProperty[parameterNum];

        for (int i = 0; i < parameterNum; i++) {
            typeProperties[i] = new TypeProperty(parameterTypes[i], false);
        }

        Map<Class<?>, Type> genericTypeMap = new HashMap<>();

        //使用递归将方法参数类型排序
        Map<Integer, ParameterType> parameterTypeMap = processParameterTypes(typeProperties, null, null,
                genericTypeMap);

        // 构建Class之前准备好所有需要的泛型
        Map<Integer, TypeDefinition> preparedGeneric = preparedGenericBeforeBuildClass(parameterTypeMap);

        String[] parameterNames = new String[parameterNum];

        for (int i = 0; i < parameterNum; i++) {
            parameterNames[i] = "arg" + i;
        }

        //构建Class
        Class<?> dynamicClass = buildClass(parameterNames, preparedGeneric);

        return dynamicClass;

    }


    /**
     * 构建Class
     *
     * @param parameterNames
     * @param preparedGeneric
     * @return
     */
    private static Class<?> buildClass(String[] parameterNames, Map<Integer, TypeDefinition> preparedGeneric) {
        DynamicType.Builder dynamicBuilder = new ByteBuddy()
                .subclass(SwaggerExample.class);

        AtomicInteger index = new AtomicInteger(0);
        for (Map.Entry<Integer, TypeDefinition> entry : preparedGeneric.entrySet()) {
            final int currentIndex = index.getAndIncrement();
            TypeDefinition generic = entry.getValue();
            //构建field
            dynamicBuilder = dynamicBuilder.defineProperty(parameterNames[currentIndex], generic);
        }

        DynamicType.Unloaded<?> dynamicType = dynamicBuilder.make();

        //写入到本地目录
//        try {
//            dynamicType.saveIn(new File("target/classes"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return dynamicType.load(SwaggerExampleClassBuilder.class.getClassLoader(),
                ClassLoadingStrategy.Default.INJECTION).getLoaded();
    }

    /**
     * 构建Class之前准备好所有需要的泛型
     *
     * @param parameterTypeMap
     * @return
     */
    private static Map<Integer, TypeDefinition> preparedGenericBeforeBuildClass(Map<Integer, ParameterType> parameterTypeMap) {
        //用rawTypeId进行一次group by
        Map<Integer, List<ParameterType>> rawTypeIdGroupingMap = parameterTypeMap.entrySet().stream()
                .filter(t -> t != null)
                .map(Map.Entry::getValue)
                .collect(Collectors.groupingBy(ParameterType::getRawTypeId));

        Integer currentRawTypeId = null;
        final Map<Integer, TypeDefinition> preparedGeneric = new TreeMap<>();

        while ((currentRawTypeId = rawTypeIdGroupingMap.keySet()
                .stream().filter(i -> !NON_RAW_TYPE.equals(i))
                .max(Integer::compare)
                .orElse(null)) != null) {
            //构建泛型
            preparedGeneric.putAll(buildGeneric(parameterTypeMap, rawTypeIdGroupingMap, currentRawTypeId, null));
        }

        //此时rawTypeIdGroupingMap中还剩下key为NON_RAW_TYPE的数据
        List<ParameterType> remainParameterType = rawTypeIdGroupingMap.get(NON_RAW_TYPE);

        //将剩余的泛型中的RawType排除,并转换为TypeDescription
        Map<Integer, TypeDefinition> remainGeneric =
                remainParameterType.stream().filter(t -> !preparedGeneric.containsKey(t.getId()))
                        .map(SwaggerExampleClassBuilder::processRemainParameterType)
                        .collect(Collectors.toMap(ParameterType::getId,
                                // t -> TypeDescription.ForLoadedType.of(t.getType()), (a, b) -> a, TreeMap::new));
                                //为了解决版本冲突将bytebuddy版本改成了1.7.11
                                t -> new TypeDescription.ForLoadedType(t.getType()), (a, b) -> a, TreeMap::new));


        preparedGeneric.putAll(remainGeneric);
        return preparedGeneric;
    }

    /**
     * 处理剩余的RemainParameterType
     *
     * @return
     */
    private static ParameterType processRemainParameterType(ParameterType parameterType) {
        //如果是private类型
        if (parameterType.isPrivate) {
            parameterType = new ParameterType(parameterType.getId(), parameterType.getRawTypeId(), Object.class, null
                    , parameterType.isPrivate(), parameterType.isArray());
        }
        return parameterType;

    }

    /**
     * 构建泛型
     *
     * @param parameterTypeMap
     * @param rawTypeIdGroupingMap
     * @param currentRawTypeId
     * @param typeMaps
     * @return
     */
    private static Map<Integer, TypeDefinition> buildGeneric(Map<Integer, ParameterType> parameterTypeMap,
                                                             Map<Integer, List<ParameterType>> rawTypeIdGroupingMap,
                                                             Integer currentRawTypeId,
                                                             Map<Integer, GenericType> typeMaps) {

        Map<Integer, TypeDefinition> preparedGeneric = new TreeMap<>();
        if (typeMaps == null) {
            typeMaps = new TreeMap<>();
        }

        //排序
        List<ParameterType> parameterTypes = rawTypeIdGroupingMap.get(currentRawTypeId)
                .stream().filter(t -> t != null)
                .sorted(Comparator.comparing(ParameterType::getId))
                .collect(Collectors.toList());

        //移除
        rawTypeIdGroupingMap.remove(currentRawTypeId);

        if (parameterTypes != null && !parameterTypes.isEmpty()) {

            //根据currentRawTypeId获取RawType
            ParameterType parameterType = parameterTypeMap.get(currentRawTypeId);

            //是否是private
            boolean isPrivate = parameterType.isPrivate();

            Class<?> rawType = parameterType.getType();

            Integer parentRowTypeId = parameterType.getRawTypeId();

            //泛型
            TypeDefinition generic;

            //如果是private则不解析该Class使用默认Object.class
            if (isPrivate) {
                rawType = Object.class;
                // 泛型
                // generic = TypeDescription.ForLoadedType.of(rawType);
                //为了解决版本冲突将bytebuddy版本改成了1.7.11
                generic = new TypeDescription.ForLoadedType(rawType);
            } else {

                Type ownerType = parameterType.getOwnerType();

                boolean isArray = parameterType.isArray();

                //注意参数顺序
                Map<Integer, TypeDefinition> idTypeDefinitionMap =
                        parameterTypes.stream().collect(Collectors.toMap(ParameterType::getId,
                                //   e -> TypeDescription.ForLoadedType.of(e.getType()), (a, b) -> b, TreeMap::new));
                                //为了解决版本冲突将bytebuddy版本改成了1.7.11
                                e -> new TypeDescription.ForLoadedType(e.getType()), (a, b) -> b, TreeMap::new));
                if (typeMaps != null && !typeMaps.isEmpty()) {
                    GenericType genericType = typeMaps.get(currentRawTypeId);
                    if (genericType != null) {
                        idTypeDefinitionMap.put(genericType.getId(), genericType.getGeneric().asGenericType());
                    }
                }
                List<TypeDefinition> typeDefinitions =
                        idTypeDefinitionMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());

                //ownerType泛型
                TypeDescription.Generic ownerTypeGeneric =
                        Optional.ofNullable(ownerType).map(TypeDefinition.Sort::describe).orElse(null);

                //数组
                if (isArray) {
                    generic =
                            TypeDescription.Generic.Builder.parameterizedType(
                                    //TypeDescription.ForLoadedType.of(rawType)
                                    //为了解决版本冲突将bytebuddy版本改成了1.7.11
                                    new TypeDescription.ForLoadedType(rawType)
                                    , ownerTypeGeneric, typeDefinitions).asArray().build();

                    //非数组
                } else {
                    generic =
                            TypeDescription.Generic.Builder.parameterizedType(
                                    // TypeDescription.ForLoadedType.of(rawType)
                                    //为了解决版本冲突将bytebuddy版本改成了1.7.11
                                    new TypeDescription.ForLoadedType(rawType)
                                    , ownerTypeGeneric, typeDefinitions).build();
                }

            }

            //没有RawType
            if (NON_RAW_TYPE.equals(parentRowTypeId)) {
                preparedGeneric.put(currentRawTypeId, generic);
                //还有RowType需要继续拼装泛型
            } else {
                typeMaps.put(parentRowTypeId, new GenericType(currentRawTypeId, parentRowTypeId, generic));
                preparedGeneric.putAll(buildGeneric(parameterTypeMap, rawTypeIdGroupingMap, parentRowTypeId, typeMaps));
            }
        }

        return preparedGeneric;


    }

    /**
     * 使用递归将方法参数类型排序
     *
     * @param parameterTypes
     * @param id
     * @param rawTypeId
     * @return
     */
    private static Map<Integer, ParameterType> processParameterTypes(TypeProperty[] parameterTypes, AtomicInteger id,
                                                                     Integer rawTypeId,
                                                                     Map<Class<?>, Type> genericTypeMap) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return Collections.EMPTY_MAP;
        }
        if (id == null) {
            id = new AtomicInteger(0);
        }
        if (rawTypeId == null) {
            rawTypeId = NON_RAW_TYPE;
        }
        Map<Integer, ParameterType> parameterTypeMap = new TreeMap<>();
        for (TypeProperty typeProperty : parameterTypes) {
            Integer currentId = id.incrementAndGet();

            Type type = typeProperty.getType();

            boolean isArray = typeProperty.isArray();

            if (type instanceof Class<?>) {

                Class<?> clazz = (Class<?>) type;

                //数组
                if (clazz.isArray()) {
                    boolean isPrivate = Modifier.isPrivate(clazz.getModifiers());
                    parameterTypeMap.put(currentId, new ParameterType(currentId, rawTypeId, clazz, null,
                            isPrivate, isArray));
                    //普通class
                } else {
                    boolean isPrivate = Modifier.isPrivate(clazz.getModifiers());
                    parameterTypeMap.put(currentId, new ParameterType(currentId, rawTypeId, clazz, null,
                            isPrivate, isArray));
                }
                // List<> Map<>
            } else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;

                //TODO 确认是否都是class
                Class<?> typeClazz = (Class<?>) parameterizedType.getRawType();

                Type ownerType = parameterizedType.getOwnerType();

                //获取带泛型的参数类型
                Type[] genericTypes = parameterizedType.getActualTypeArguments();

                boolean isPrivate = Modifier.isPrivate(typeClazz.getModifiers());

                //收集已经处理好的parameterType
                parameterTypeMap.put(currentId, new ParameterType(currentId, rawTypeId, typeClazz, ownerType,
                        isPrivate, isArray));

                int size = genericTypes.length;

                TypeProperty[] allTypes = new TypeProperty[size];

                for (int i = 0; i < genericTypes.length; i++) {
                    allTypes[i] = new TypeProperty(genericTypes[i], false);
                }

                //递归处理泛型
                parameterTypeMap.putAll(processParameterTypes(allTypes, id, currentId, genericTypeMap));

                // T
            } else if (type instanceof TypeVariable) {

                TypeVariable typeVariable = (TypeVariable) type;

                Type genericType = SwaggerReflectUtils.getActualTypeByTypeVariable(typeVariable, genericTypeMap);

                parameterTypeMap.putAll(processParameterTypes(new TypeProperty[]{new TypeProperty(genericType,
                                isArray)},
                        id, rawTypeId, genericTypeMap));

                // ? extends Object
            } else if (type instanceof WildcardType) {

                WildcardType wildcardType = (WildcardType) type;
                Type genericType = SwaggerReflectUtils.getActualTypeByWildcardType(wildcardType);
                //收集已经处理好的parameterType

                parameterTypeMap.putAll(processParameterTypes(new TypeProperty[]{new TypeProperty(genericType,
                                isArray)}, id, rawTypeId,
                        genericTypeMap));


                // T []
            } else if (type instanceof GenericArrayType) {

                GenericArrayType genericArrayType = (GenericArrayType) type;

                Type genericComponentType = (genericArrayType).getGenericComponentType();
                //设置数组标记isArray = true
                parameterTypeMap.putAll(processParameterTypes(new TypeProperty[]{new TypeProperty(genericComponentType, true)},
                        id, rawTypeId, genericTypeMap));

            }
        }
        return parameterTypeMap;
    }


    /**
     * 参数类型
     */
    private static class ParameterType {

        /**
         * 方法参数类型的ID主要用于排序
         */
        private Integer id;

        /**
         * 用该类型修饰的RawType的ID
         */
        private Integer rawTypeId;

        /**
         * 参数的类型Class
         */
        private Class<?> type;

        /**
         * 参数的ownerType
         */
        private Type ownerType;

        /**
         * 是否private
         */
        private boolean isPrivate;

        /**
         * 是否是数组
         */
        private boolean isArray;

        public ParameterType(Integer id, Integer rawTypeId, Class<?> type, Type ownerType, boolean isPrivate,
                             boolean isArray) {
            this.id = id;
            this.rawTypeId = rawTypeId;
            this.type = type;
            this.ownerType = ownerType;
            this.isPrivate = isPrivate;
            this.isArray = isArray;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getRawTypeId() {
            return rawTypeId;
        }

        public void setRawTypeId(Integer rawTypeId) {
            this.rawTypeId = rawTypeId;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> type) {
            this.type = type;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        public void setOwnerType(Type ownerType) {
            this.ownerType = ownerType;
        }

        public boolean isPrivate() {
            return isPrivate;
        }

        public void setPrivate(boolean aPrivate) {
            isPrivate = aPrivate;
        }

        public boolean isArray() {
            return isArray;
        }

        public void setArray(boolean array) {
            isArray = array;
        }

    }


    /**
     * 泛型类型
     */
    private static class GenericType {
        /**
         * 方法参数类型的ID主要用于排序
         */
        private Integer id;

        /**
         * 用该类型修饰的RawType的ID
         */
        private Integer rawTypeId;

        /**
         * 构建class时,Byte Buddy需要的field类型
         */
        private TypeDefinition generic;

        public GenericType(Integer id, Integer rawTypeId, TypeDefinition generic) {
            this.id = id;
            this.rawTypeId = rawTypeId;
            this.generic = generic;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getRawTypeId() {
            return rawTypeId;
        }

        public void setRawTypeId(Integer rawTypeId) {
            this.rawTypeId = rawTypeId;
        }

        public TypeDefinition getGeneric() {
            return generic;
        }

        public void setGeneric(TypeDefinition generic) {
            this.generic = generic;
        }

    }

    private static class TypeProperty {
        private Type type;

        private boolean isArray;

        public TypeProperty(Type type, boolean isArray) {
            this.type = type;
            this.isArray = isArray;
        }


        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public boolean isArray() {
            return isArray;
        }

        public void setArray(boolean array) {
            isArray = array;
        }
    }

}

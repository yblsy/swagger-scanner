package com.valsong.swagger.util;

import com.valsong.swagger.exception.SwaggerScannerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.valsong.swagger.constant.SwaggerScannerConstants.COLLECTION_FIRST_INDEX;
import static com.valsong.swagger.constant.SwaggerScannerConstants.COLLECTION_SECOND_INDEX;


/**
 * 获取Type的所有field的Type
 *
 * @author Val Song
 */
public class FieldTypesDiscover {

    private static final Logger logger = LoggerFactory.getLogger(FieldTypesDiscover.class);

    private FieldTypesDiscover() {
    }

    private Set<Type> types = new HashSet<>();

    /**
     * 递归找出field的type
     *
     * @param type  方法的参数或者返回值
     * @param clazz 执行方法的class
     * @return
     */
    public static Set<Type> getFieldTypes(Type type, Class<?> clazz) {

        FieldTypesDiscover fieldTypesDiscover = new FieldTypesDiscover();

        fieldTypesDiscover.discover(type, SwaggerReflectUtils.getGenericTypeMap(clazz));

        return fieldTypesDiscover.types;
    }


    /**
     * 递归找出field的type
     *
     * @param type
     * @param genericTypeMap
     * @param <T>
     * @return
     */
    private <T> void discover(Type type, Map<Class<?>, Type> genericTypeMap) {

        if (genericTypeMap == null) {
            genericTypeMap = new HashMap<>();
        }

        Class<T> clazz = null;
        //class
        if (type instanceof Class) {
            //数组
            if (((Class<?>) type).isArray()) {
                Class<?> componentType = ((Class<?>) type).getComponentType();

                clazz = (Class<T>) componentType;

                //普通class
            } else {
                clazz = (Class<T>) type;

            }
            // List<> Map<>
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            clazz = (Class<T>) rawType;
            // T
        } else if (type instanceof TypeVariable) {
            Type actualType = SwaggerReflectUtils.getActualTypeByTypeVariable((TypeVariable) type, genericTypeMap);
            discover(actualType, genericTypeMap);
            return;
            // ? extends Object
        } else if (type instanceof WildcardType) {
            Type actualType = SwaggerReflectUtils.getActualTypeByWildcardType((WildcardType) type);
            discover(actualType, genericTypeMap);
            return;
            // T []
        } else if (type instanceof GenericArrayType) {
            Type genericComponentType = ((GenericArrayType) type).getGenericComponentType();
            discover(genericComponentType, genericTypeMap);
            return;
        }

        FieldTypeProcessor.process(clazz, type, genericTypeMap, this);
    }


    /**
     * 处理未知类型
     *
     * @param clazz
     * @param type
     * @param genericTypeMap
     * @param <T>
     * @return
     */
    private <T> void resolve(Class<T> clazz, Type type, Map<Class<?>, Type> genericTypeMap) {


        //void或接口或者抽象类返回空
        if (clazz.equals(void.class) || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || Object.class.equals(clazz)) {
            return;
        }

        if (types.contains(type)) {
            return;
        }

        types.add(type);
        types.add(clazz);

        BeanInfo beanInfo = null;

        try {
            beanInfo = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            throw new SwaggerScannerException("class:" + clazz + " getBeanInfo failed.", e);
        }

        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Class<?> propertyType = propertyDescriptor.getPropertyType();

            //setter
            Method writeMethod = propertyDescriptor.getWriteMethod();

            //跳过class属性，跳过与自身类相同的属性，没有set方法跳过
            if (Class.class.equals(propertyType) || clazz.equals(propertyType) || writeMethod == null) {
                continue;
            }

            final String fieldName = propertyDescriptor.getName();

            Field currentField = SwaggerReflectUtils.findDeclaredField(clazz, fieldName);

            if (currentField == null) {
                continue;
            }

            Type fieldType = currentField.getGenericType();

            genericTypeMap.putAll(SwaggerReflectUtils.getGenericTypeMap(type));

            List<Type> allTypeFromField = SwaggerReflectUtils.findAllTypeFromType(fieldType, genericTypeMap);

            //检测自关联
            if (allTypeFromField != null && allTypeFromField.contains(clazz)) {
                continue;
            }

            discover(fieldType, genericTypeMap);

        }


    }

    /**
     * 处理field type
     */
    private enum FieldTypeProcessor {
        SHORT(short.class),
        SHORT_WRAPPER(Short.class),
        INT(int.class),
        INTEGER(Integer.class),
        BIG_INTEGER(BigInteger.class),
        LONG(long.class),
        LONG_WRAPPER(Long.class),
        FLOAT(float.class),
        FLOAT_WRAPPER(Float.class),
        DOUBLE(double.class),
        DOUBLE_WRAPPER(Double.class),
        BIG_DECIMAL(BigDecimal.class),
        CHAR(char.class),
        CHARACTER(Character.class),
        BOOLEAN(boolean.class),
        BOOLEAN_WRAPPER(Boolean.class),
        BYTE(byte.class),
        BYTE_WRAPPER(Byte.class),
        STRING(String.class),
        DATE(Date.class),
        NUMBER(Number.class),
        COLLECTION(Collection.class) {
            @Override
            public void doProcess(Type type, Map<Class<?>, Type> genericTypeMap,
                                  FieldTypesDiscover fieldTypesDiscover) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    final Type rawType = parameterizedType.getRawType();
                    fieldTypesDiscover.types.add(rawType);
                    final Type valType = parameterizedType.getActualTypeArguments()[COLLECTION_FIRST_INDEX];
                    fieldTypesDiscover.discover(valType, genericTypeMap);
                }
            }
        },
        MAP(Map.class) {
            @Override
            public void doProcess(Type type, Map<Class<?>, Type> genericTypeMap,
                                  FieldTypesDiscover fieldTypesDiscover) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    final Type rawType = parameterizedType.getRawType();
                    fieldTypesDiscover.types.add(rawType);
                    final Type keyType = parameterizedType.getActualTypeArguments()[COLLECTION_FIRST_INDEX];
                    fieldTypesDiscover.discover(keyType, genericTypeMap);
                    final Type valType = parameterizedType.getActualTypeArguments()[COLLECTION_SECOND_INDEX];
                    fieldTypesDiscover.discover(valType, genericTypeMap);
                }
            }
        };

        private final Class<?> clazz;

        public Class<?> getClazz() {
            return clazz;
        }

        FieldTypeProcessor(Class<?> clazz) {
            this.clazz = clazz;
        }

        /**
         * 处理
         *
         * @param type
         * @param genericTypeMap
         * @return
         */
        public void doProcess(Type type, Map<Class<?>, Type> genericTypeMap, FieldTypesDiscover fieldTypesDiscover) {
            //do nothing
        }


        /**
         * 获取field类型
         *
         * @param clazz
         * @param type
         * @param genericTypeMap
         * @param <T>
         * @return
         */
        public static <T> void process(Class<T> clazz, Type type, Map<Class<?>, Type> genericTypeMap,
                                       FieldTypesDiscover fieldTypesDiscover) {
            FieldTypeProcessor[] fieldTypeProcessors = FieldTypeProcessor.values();
            try {
                for (FieldTypeProcessor fieldTypeProcessor : fieldTypeProcessors) {

                    try {
                        if (fieldTypeProcessor.clazz.isAssignableFrom(clazz)) {
                            fieldTypeProcessor.doProcess(type, genericTypeMap, fieldTypesDiscover);
                            return;
                        }
                    } catch (Exception e) {
                        throw new SwaggerScannerException(e);
                    }
                }
            } catch (Exception e) {
                throw new SwaggerScannerException(e);
            }
            fieldTypesDiscover.resolve(clazz, type, genericTypeMap);
        }

    }

}

package com.neo.util.modifyinfo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.base.MoreObjects;
import com.neo.util.ArrayUtils;
import com.neo.util.ReflectUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 组装修改信息工具类
 * 
 * 处理器优先级：BuilderModifyInfoHandle.Builder#putFieldNameProcessHandleMapping() > Property#typeProcessHandleUsing() > BuilderModifyInfoHandle.Builder#registerProcessHandle() > 默认
 * 属性名称优先级：BuilderModifyInfoHandle.Builder#putFieldTitleMapping() > Property#nameAnnotationClass() > Property#name() > BuilderModifyInfoHandle.Builder#configGlobalNameAnnotation()
 *
 * 
 * @Author: Neo
 * @Date: 2019/11/22 22:49
 * @Version: 1.0
 */
public class BuilderModifyInfoHandle {

    private BuilderModifyInfoHandle(){}
    
    private static final String[] EMPTY_ARRAY = new String[0];
    private static TypeProcessHandle DATE_TYPE_PROCESS_HANDLE = new DateTypeProcessHandle();
    private static TypeProcessHandle BIG_DECIMAL_TYPE_PROCESS_HANDLE = new BigDecimalTypeProcessHandle();
    private static TypeProcessHandle DEFAULT_TYPE_PROCESS_HANDLE = new DefaultTypeProcessHandle();


    /** 修改前的对象 */
    private Object oldObject;
    /** 修改后的对象 */
    private Object newObject;
    /** 需要比较的字段 */
    private String[] compareFields;
    /** 比较时需要忽略的字段 */
    private String[] ignoreCompareFields;
    /** 映射 ： 类型 - 处理器 */
    private Map<Class<?>, TypeProcessHandle> typeProcessHandleMap;
    /** 映射 ： 属性名 - 处理器 */
    private Map<String , TypeProcessHandle> fieldNameProcessHandleMap;

    /** 存储修改前值的 Key */
    private String oldValueKey;
    /** 存储修改后值的 Key */
    private String newValueKey;
    /** 存储属性名的 Key */
    private String fieldNameKey;
    /** 存储属性标题的 KEY */
    private String fieldTitleKey;
    /** 是否需要组装 null 值 */
    private boolean isBuilderNullValue;

    /** 映射 ： 属性名 - 属性标题 */
    private Map<String, String> fieldTitleMapping;
    
    /** 全局名称标注注解类，单独使用无效，需配合 globalNameAnnotationClassField 一起使用 */
    private Class<? extends Annotation> globalNameAnnotationClass;
    /** 全局名称标注注解类属性，单独使用无效，需配合 globalNameAnnotationClass 一起使用 */
    private String globalNameAnnotationClassField;

    private BuilderModifyInfoHandle(Builder builder) {
        setOldObject(builder.oldObject);
        setNewObject(builder.newObject);
        setCompareFields(builder.compareFields);
        setTypeProcessHandleMap(builder.typeProcessHandleMap);
        setFieldNameProcessHandleMap(builder.fieldNameProcessHandleMap);
        setOldValueKey(builder.oldValueKey);
        setNewValueKey(builder.newValueKey);
        setFieldNameKey(builder.fieldNameKey);
        setFieldTitleKey(builder.fieldTitleKey);
        setFieldTitleMapping(builder.fieldTitleMapping);
        setIgnoreCompareFields(builder.ignoreCompareFields);
        setGlobalNameAnnotationClass(builder.globalNameAnnotationClass);
        setGlobalNameAnnotationClassField(builder.globalNameAnnotationClassField);
    }


    /**
     * 组装对象变动信息
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:59
     * @Version: 1.0
     */
    public JSONArray builderModifyInfo(Object oldObject, Object newObject, String... compareFields) {
        if (Objects.isNull(newObject) || Objects.isNull(oldObject)) {
            throw new RuntimeException("参数异常");
        }
        JSONArray jsonArray = new JSONArray();
        try {
            for (String field : compareFields) {

                Object oldValue = ReflectUtils.getValueByFieldName(oldObject, field);
                Object newValue = ReflectUtils.getValueByFieldName(newObject, field);
                
                if (!isDifferent(oldObject, newObject, field, newValue, oldValue)) {
                    continue;
                }
                jsonArray.add(builderModifyInfo(oldObject, newObject, field, newValue, oldValue));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }


    /**
     * 组装对象变动信息
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:57
     * @Version: 1.0
     */
    public JSONObject builderModifyInfo(Object oldObject, Object newObject, String fieldName, Object oldValue, Object newValue)
            throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        String title = getFieldTitle(newObject, fieldName);
        if (StringUtils.isBlank(title)) {
            title = getFieldTitle(oldObject, fieldName);
        }
        if (StringUtils.isBlank(title)) {
            title = getFieldTitle(oldObject, fieldName);
        }

        // 获取处理器
        TypeProcessHandle handle = getTypeProcessHandle(oldObject, newObject, fieldName);
        if (Objects.isNull(handle)) {
            throw new RuntimeException("没有找到处理器");
        }

        // 调用处理器格式化方式
        Object after = handle.format(oldObject.getClass(), fieldName, oldValue);
        Object before = handle.format(newObject.getClass(), fieldName, newValue);


        JSONObject json = new JSONObject();
        json.put(this.fieldTitleKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(title, StringUtils.EMPTY) : title);
        json.put(this.fieldNameKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(fieldName, StringUtils.EMPTY) : fieldName);
        json.put(this.oldValueKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(before, StringUtils.EMPTY) : before);
        json.put(this.newValueKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(after, StringUtils.EMPTY) : after);
        return json;
    }


    /**
     * 得到类型处理器
     * 处理器优先级：BuilderModifyInfoHandle.Builder#putFieldNameProcessHandleMapping() > Property#typeProcessHandleUsing() > BuilderModifyInfoHandle.Builder#registerProcessHandle() > 默认
     * @Author: Neo
     * @Date: 2019/11/24 17:41
     * @Version: 1.0
     */
    private TypeProcessHandle getTypeProcessHandle(Object oldObject, Object newObject, String fieldName) throws IllegalAccessException, InstantiationException {
        // 判断比较值得类型是否一致，都不为 null 且类型不一致则直接返回默认处理器
        Class<?> oldObjectFieldClass = getObjectFieldClass(oldObject, fieldName);
        Class<?> newObjectFieldClass = getObjectFieldClass(newObject, fieldName);

        if (!Objects.isNull(oldObjectFieldClass) && !Objects.isNull(newObjectFieldClass) && !oldObjectFieldClass.equals(newObjectFieldClass)) {
            return DEFAULT_TYPE_PROCESS_HANDLE;
        }

        // 1.BuilderModifyInfoHandle.Builder#putFieldNameProcessHandleMapping()
        TypeProcessHandle handle;
        if (!Objects.isNull(this.fieldNameProcessHandleMap) && !this.fieldNameProcessHandleMap.isEmpty()) {
            handle = this.fieldNameProcessHandleMap.get(fieldName);
            if (!Objects.isNull(handle)) {
                return handle;
            }
        }


        // 2.Property#typeProcessHandleUsing()
        handle = getPropertyDefinitionProcessHandle(newObject, fieldName);
        if (!Objects.isNull(handle)) {
            return handle;
        }
        handle = getPropertyDefinitionProcessHandle(oldObject, fieldName);
        if (!Objects.isNull(handle)) {
            return handle;
        }
        Class<?> targetType = MoreObjects.firstNonNull(newObjectFieldClass, oldObjectFieldClass);

        // 3.BuilderModifyInfoHandle.Builder#registerProcessHandle()
        if (!Objects.isNull(this.typeProcessHandleMap) && !this.typeProcessHandleMap.isEmpty()) {
            handle = this.typeProcessHandleMap.get(targetType);
            if (!Objects.isNull(handle)) {
                return handle;
            }
        }


        // 4.默认
        if (BigDecimal.class.isAssignableFrom(targetType)) {
            return BIG_DECIMAL_TYPE_PROCESS_HANDLE;
        }
        if (Date.class.isAssignableFrom(targetType)) {
            return DATE_TYPE_PROCESS_HANDLE;
        }
        return DEFAULT_TYPE_PROCESS_HANDLE;
    }

    /**
     * 获取对象属性类
     *
     * @Author: Neo
     * @Date: 2019/11/25 10:27
     * @Version: 1.0
     */
    private Class<?> getObjectFieldClass(Object object, String fieldName) {
        if (Objects.isNull(object) || StringUtils.isBlank(fieldName)) {
            return null;
        }

        if (Map.class.isAssignableFrom(object.getClass())) {
            Map map = (Map) object;
            Object value = map.get(fieldName);
            if (Objects.isNull(value)) {
                return null;
            }
            return value.getClass();
        }

        Field field = ReflectUtils.getFieldByFieldName(object, fieldName);
        if (Objects.isNull(field)) {
            return null;
        }

        return field.getType();
    }
    
    /**
     * 获取属性注解定义的处理器
     *
     * @Author: Neo
     * @Date: 2019/11/25 10:08
     * @Version: 1.0
     */
    private TypeProcessHandle getPropertyDefinitionProcessHandle(Object object,  String fieldName) throws IllegalAccessException, InstantiationException {
        if (Objects.isNull(object) || StringUtils.isBlank(fieldName) || Map.class.isAssignableFrom(object.getClass())) {
            return null;
        }

        Field field = ReflectUtils.getFieldByFieldName(object.getClass(), fieldName);
        if (Objects.isNull(field)) {
            return null;
        }
        // 获取属性注解处理类
        Property annotation = field.getAnnotation(Property.class);
        if (Objects.isNull(annotation) || Void.class.equals(annotation.typeProcessHandleUsing())) {
            return null;
        }

        if (!TypeProcessHandle.class.isAssignableFrom(annotation.typeProcessHandleUsing())) {
            throw new RuntimeException(Property.class + ".typeProcessHandleUsing:" + annotation.typeProcessHandleUsing() + "不是 TypeProcessHandle 的实现类");
        }
        return (TypeProcessHandle) annotation.typeProcessHandleUsing().newInstance();
    }

    /**
     * 得到属性标题
     * <p>
     * 属性名称优先级：BuilderModifyInfoHandle.Builder#putFieldTitleMapping() > Property#nameAnnotationClass() > Property#name() > BuilderModifyInfoHandle.Builder#configGlobalNameAnnotation()
     *
     * @Author: Neo
     * @Date: 2019/11/22 22:30
     * @Version: 1.0
     */
    public String getFieldTitle(Object object, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String title = StringUtils.EMPTY;
        // 1.BuilderModifyInfoHandle.Builder#putFieldTitleMapping()
        if (!Objects.isNull(this.fieldTitleMapping) && !this.fieldTitleMapping.isEmpty()) {
            title = this.fieldTitleMapping.get(fieldName);
        }

        if (StringUtils.isNotBlank(title)) {
            return title;
        }

        if (Objects.isNull(object) || Map.class.isAssignableFrom(object.getClass())) {
            return null;
        }

        Field field = ReflectUtils.getFieldByFieldName(object.getClass(), fieldName);
        if (Objects.isNull(field)) {
            return null;
        }

        
        if (field.isAnnotationPresent(Property.class)) {
            Property property = field.getAnnotation(Property.class);
            // 2.Property#nameAnnotationClass()
            if (StringUtils.isNotBlank(property.nameAnnotationClassField())) {
                // 获取 Property.nameAnnotationClassField 标注的注解属性值
                title = getNameAnnotationTitle(field, property);

                if (StringUtils.isNotBlank(title)) {
                    return title;
                }
            }

            // 3.Property#name()
            if (StringUtils.isNotBlank(property.name())) {
                return property.name();
            }
        }

        // 4.BuilderModifyInfoHandle.Builder#configGlobalNameAnnotation()
        if (!Objects.isNull(this.globalNameAnnotationClass)
                && StringUtils.isNotBlank(this.globalNameAnnotationClassField)
                && field.isAnnotationPresent(this.globalNameAnnotationClass)) {
            Object annotationFieldValue = getAnnotationFieldValue(field.getAnnotation(this.globalNameAnnotationClass), this.globalNameAnnotationClassField);
            if (!Objects.isNull(annotationFieldValue)) {
                return TypeUtils.castToString(annotationFieldValue);
            }
        }
        return null;
    }

    /**
     * 获取 Property.nameAnnotationClassField 标注的注解属性值
     *
     * @Author: Neo
     * @Date: 2019/11/23 16:26
     * @Version: 1.0
     */
    public String getNameAnnotationTitle(Field field, Property property) throws NoSuchFieldException, IllegalAccessException {
        if (Void.class.equals(property.nameAnnotationClass())) {
            return null;
        }
        if (!Annotation.class.isAssignableFrom(property.nameAnnotationClass())) {
            throw new RuntimeException(Property.class + ".nameAnnotationClass:" + property.nameAnnotationClass() + "不是注解类");
        }
        Class<? extends Annotation> nameAnnotationClass = (Class<? extends Annotation>) property.nameAnnotationClass();

        // 获取 Property.nameAnnotationClass 标注的注解代理实例
        Annotation nameAnnotation = field.getAnnotation(nameAnnotationClass);
        Object annotationFieldValue = getAnnotationFieldValue(nameAnnotation, property.nameAnnotationClassField());
        return TypeUtils.castToString(annotationFieldValue);
    }
    
    /**
     * 获取代理注解的属性值
     *
     * @Author: Neo
     * @Date: 2019/11/25 17:07
     * @Version: 1.0
     */
    public Object getAnnotationFieldValue(Annotation annotation , String field) throws IllegalAccessException,NoSuchFieldException{
        //获取代理实例所持有的 InvocationHandler
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(annotation);

        // 获取 AnnotationInvocationHandler 的 memberValues 字段
        Field memberValuesField = invocationHandler.getClass().getDeclaredField("memberValues");

        // 打开访问权限
        memberValuesField.setAccessible(true);

        // 获取 memberValues
        Map memberValues = (Map) memberValuesField.get(invocationHandler);

        // 获取 Property.nameAnnotationClassField 标注的注解属性值
        return memberValues.get(field);
    }


    /**
     * 比较值
     * 相同返回：false
     * 不相同返回：true
     *
     * @Author: Neo
     * @Date: 2019/11/22 17:58
     * @Version: 1.0
     */
    public boolean isDifferent(Object oldObject, Object newObject, String field, Object oldValue, Object newValue) throws IllegalAccessException, InstantiationException {
        // 获取处理器
        TypeProcessHandle handle = getTypeProcessHandle(oldObject, newObject, field);
        if (Objects.isNull(handle)) {
            throw new RuntimeException("没有找到处理器");
        }

        return handle.isDifferent(oldValue, newValue);
    }


    public Object getNewObject() {
        return newObject;
    }

    public void setNewObject(Object newObject) {
        this.newObject = newObject;
    }

    public Object getOldObject() {
        return oldObject;
    }

    public void setOldObject(Object oldObject) {
        this.oldObject = oldObject;
    }

    public String[] getCompareFields() {
        return compareFields;
    }

    public void setCompareFields(String[] compareFields) {
        this.compareFields = compareFields;
    }

    public String[] getIgnoreCompareFields() {
        return ignoreCompareFields;
    }

    public void setIgnoreCompareFields(String[] ignoreCompareFields) {
        this.ignoreCompareFields = ignoreCompareFields;
    }

    public Map<Class<?>, TypeProcessHandle> getTypeProcessHandleMap() {
        return typeProcessHandleMap;
    }

    public void setTypeProcessHandleMap(Map<Class<?>, TypeProcessHandle> typeProcessHandleMap) {
        this.typeProcessHandleMap = typeProcessHandleMap;
    }

    public Map<String, TypeProcessHandle> getFieldNameProcessHandleMap() {
        return fieldNameProcessHandleMap;
    }

    public void setFieldNameProcessHandleMap(Map<String, TypeProcessHandle> fieldNameProcessHandleMap) {
        this.fieldNameProcessHandleMap = fieldNameProcessHandleMap;
    }

    public String getOldValueKey() {
        return oldValueKey;
    }

    public void setOldValueKey(String oldValueKey) {
        this.oldValueKey = oldValueKey;
    }

    public String getNewValueKey() {
        return newValueKey;
    }

    public void setNewValueKey(String newValueKey) {
        this.newValueKey = newValueKey;
    }

    public String getFieldNameKey() {
        return fieldNameKey;
    }

    public void setFieldNameKey(String fieldNameKey) {
        this.fieldNameKey = fieldNameKey;
    }

    public String getFieldTitleKey() {
        return fieldTitleKey;
    }

    public void setFieldTitleKey(String fieldTitleKey) {
        this.fieldTitleKey = fieldTitleKey;
    }

    public boolean isBuilderNullValue() {
        return isBuilderNullValue;
    }

    public void setBuilderNullValue(boolean builderNullValue) {
        isBuilderNullValue = builderNullValue;
    }

    public Map<String, String> getFieldTitleMapping() {
        return fieldTitleMapping;
    }

    public void setFieldTitleMapping(Map<String, String> fieldTitleMapping) {
        this.fieldTitleMapping = fieldTitleMapping;
    }

    public Class<? extends Annotation> getGlobalNameAnnotationClass() {
        return globalNameAnnotationClass;
    }

    public void setGlobalNameAnnotationClass(Class<? extends Annotation> globalNameAnnotationClass) {
        this.globalNameAnnotationClass = globalNameAnnotationClass;
    }

    public String getGlobalNameAnnotationClassField() {
        return globalNameAnnotationClassField;
    }

    public void setGlobalNameAnnotationClassField(String globalNameAnnotationClassField) {
        this.globalNameAnnotationClassField = globalNameAnnotationClassField;
    }

    public static Builder Builder(Object oldObject , Object newObject) {
        Builder builder = new Builder();
        builder.oldObject(oldObject);
        builder.newObject(newObject);
        return builder;
    }

    public static Builder Builder(Object oldObject , Object newObject, String... compareFields) {
        Builder builder = new Builder();
        builder.oldObject(oldObject);
        builder.newObject(newObject);
        builder.compareFields(compareFields);
        return builder;
    }


    public static final class Builder {
        /** 修改前的对象 */
        private Object oldObject;
        /** 修改后的对象 */
        private Object newObject;
        /** 需要比较的字段 */
        private String[] compareFields;
        /** 比较时需要忽略的字段 */
        private String[] ignoreCompareFields;
        /** 映射 ： 类型 - 处理器 */
        private Map<Class<?>, TypeProcessHandle> typeProcessHandleMap;
        /** 映射 ： 属性名 - 处理器 */
        private Map<String , TypeProcessHandle> fieldNameProcessHandleMap;

        /** 存储修改前值的 Key */
        private String oldValueKey = "before";
        /** 存储修改后值的 Key */
        private String newValueKey = "after";
        /** 存储属性名的 Key */
        private String fieldNameKey = "name";
        /** 存储属性标题的 KEY */
        private String fieldTitleKey = "title";
        /** 是否需要组装 null 值 */
        private boolean isBuilderNullValue = true;

        /** 映射 ： 属性名 - 属性标题 */
        private Map<String, String> fieldTitleMapping;

        /** 全局名称标注注解类，单独使用无效，需配合 globalNameAnnotationClassField 一起使用 */
        private Class<? extends Annotation> globalNameAnnotationClass;
        /** 全局名称标注注解类属性，单独使用无效，需配合 globalNameAnnotationClass 一起使用 */
        private String globalNameAnnotationClassField;

        public Builder() {
        }

        public Builder newObject(Object val) {
            newObject = val;
            return this;
        }

        public Builder oldObject(Object val) {
            oldObject = val;
            return this;
        }

        public Builder compareFields(String... val) {
            compareFields = val;
            return this;
        }

        public Builder ignoreCompareFields(String... val) {
            ignoreCompareFields = val;
            return this;
        }

        public Builder typeProcessHandleMap(Map<Class<?>, TypeProcessHandle> val) {
            typeProcessHandleMap = val;
            return this;
        }

        public Builder fieldNameProcessHandleMap(Map<String, TypeProcessHandle> val) {
            fieldNameProcessHandleMap = val;
            return this;
        }

        public Builder oldValueKey(String val) {
            oldValueKey = val;
            return this;
        }

        public Builder newValueKey(String val) {
            newValueKey = val;
            return this;
        }

        public Builder fieldNameKey(String val) {
            fieldNameKey = val;
            return this;
        }

        public Builder fieldTitleKey(String val) {
            fieldTitleKey = val;
            return this;
        }

        public Builder isBuilderNullValue(boolean val) {
            isBuilderNullValue = val;
            return this;
        }

        public Builder fieldTitleMapping(Map<String, String> val) {
            fieldTitleMapping = val;
            return this;
        }

        public Builder globalNameAnnotationClass(Class<? extends Annotation> val) {
            globalNameAnnotationClass = val;
            return this;
        }
        public Builder globalNameAnnotationClassField(String val) {
            globalNameAnnotationClassField = val;
            return this;
        }


        public Builder registerProcessHandle(TypeProcessHandle handle) {
            if (!Objects.isNull(handle)) {
                if (null == typeProcessHandleMap) {
                    typeProcessHandleMap = new HashMap<>(16);
                }
                typeProcessHandleMap.put(handle.supportTypeKey(), handle);
            }
            return this;
        }

        public Builder putFieldNameProcessHandleMapping(String field, TypeProcessHandle handle) {
            if (StringUtils.isNotBlank(field) && StringUtils.isNotBlank(field)) {
                if (null == fieldNameProcessHandleMap) {
                    fieldNameProcessHandleMap = new HashMap<>(16);
                }
                fieldNameProcessHandleMap.put(field, handle);
            }
            return this;
        }
        
        public Builder configGlobalNameAnnotation(Class<? extends Annotation> annotation , String field) {
            if (Objects.isNull(annotation) || StringUtils.isBlank(field)) {
                throw new RuntimeException("全局名称标注注解类和全局名称标注注解类属性都不可为空");
            }
            globalNameAnnotationClass = annotation;
            globalNameAnnotationClassField = field;
            return this;
        }

        public Builder putFieldTitleMapping(String field, String title) {
            if (StringUtils.isNotBlank(field) && StringUtils.isNotBlank(title)) {
                if (null == fieldTitleMapping) {
                    fieldTitleMapping = new HashMap<>();
                }
                fieldTitleMapping.put(field, title);
            }
            return this;
        }


        /**
         * 构建修改信息
         *
         * @Author: Neo
         * @Date: 2019/11/25 11:28
         * @Version: 1.0
         */
        public JSONArray buildInfo() {
            BuilderModifyInfoHandle handle = new BuilderModifyInfoHandle(this);
            String[] compareFields = mergeCompareFields(handle);
            if (ArrayUtils.isEmpty(compareFields)) {
                throw new RuntimeException("没有需要比较的字段");
            }
            return handle.builderModifyInfo(handle.getNewObject(), handle.getOldObject(), compareFields);
        }

        /**
         * 构建对象信息
         *
         * @Author: Neo
         * @Date: 2019/11/25 11:29
         * @Version: 1.0
         */
        public BuilderModifyInfoHandle buildObject() {
            return new BuilderModifyInfoHandle(this);
        }

        /**
         * 合并比较字段
         *
         * @Author: Neo
         * @Date: 2019/11/23 16:48
         * @Version: 1.0
         */
        private String[] mergeCompareFields(BuilderModifyInfoHandle handle) {
            // 已指定需要比较的字段并且没有需要忽略的字段
            if (ArrayUtils.isNotEmpty(handle.getCompareFields()) && ArrayUtils.isEmpty(handle.getIgnoreCompareFields())) {
                return handle.getCompareFields();
            }

            // 需要比较的字段和需要忽略的字段都指定
            if (ArrayUtils.isNotEmpty(handle.getCompareFields()) && ArrayUtils.isNotEmpty(handle.getIgnoreCompareFields())) {
                return mergeFieldName(handle.getCompareFields(), null, handle.getIgnoreCompareFields());
            }

            return mergeObjectFields(handle.getNewObject(), handle.getOldObject(), handle.ignoreCompareFields);
        }

        /**
         * 合并两个对象的属性排除忽略属性
         *
         * @Author: Neo
         * @Date: 2019/11/24 16:41
         * @Version: 1.0
         */
        private String[] mergeObjectFields(Object o1, Object o2, String[] ignoreFields) {
            if (Map.class.isAssignableFrom(o1.getClass()) && Map.class.isAssignableFrom(o2.getClass())) {
                return EMPTY_ARRAY;
            }

            if (!Map.class.isAssignableFrom(o1.getClass()) && Map.class.isAssignableFrom(o2.getClass())) {
                String[] compareFields = getFieldNames(ReflectUtils.getAllFields(o1.getClass()));
                return mergeFieldName(compareFields, null, ignoreFields);
            }

            if (Map.class.isAssignableFrom(o1.getClass()) && !Map.class.isAssignableFrom(o2.getClass())) {
                String[] compareFields = getFieldNames(ReflectUtils.getAllFields(o2.getClass()));
                return mergeFieldName(compareFields, null, ignoreFields);
            }

            if (o1.getClass().equals(o2.getClass())) {
                return getFieldNames(ReflectUtils.getAllFields(o1.getClass()));
            }

            String[] newObjectFields = getFieldNames(ReflectUtils.getAllFields(o1.getClass()));
            String[] oldObjectFields = getFieldNames(ReflectUtils.getAllFields(o2.getClass()));

            return mergeFieldName(newObjectFields, oldObjectFields, ignoreFields);
        }

        /**
         * 合并属性
         *
         * @Author: Neo
         * @Date: 2019/11/24 16:46
         * @Version: 1.0
         */
        private String[] mergeFieldName(String[] array1, String[] array2, String[] ignoreFields) {
            if (ArrayUtils.isEmpty(array1) && ArrayUtils.isEmpty(array2)) {
                return EMPTY_ARRAY;
            }
            Set<String> result = new HashSet<>(16);

            if (ArrayUtils.isNotEmpty(array1)) {
                for (String s : array1) {
                    result.add(s);
                }
            }
            if (ArrayUtils.isNotEmpty(array2)) {
                for (String s : array2) {
                    result.add(s);
                }
            }

            if (ArrayUtils.isEmpty(ignoreFields)) {
                return result.toArray(new String[0]);
            }

            for (String ignoreField : ignoreFields) {
                result.remove(ignoreField);
            }
            return result.toArray(new String[0]);
        }


        /**
         * 属性数组装换成属性名字符串数组
         *
         * @Author: Neo
         * @Date: 2019/11/24 16:46
         * @Version: 1.0
         */
        public String[] getFieldNames(Field[] fields) {
            if (ArrayUtils.isEmpty(fields)) {
                return EMPTY_ARRAY;
            }
            List<String> result = new ArrayList<>(fields.length);
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                // 判断改属性是否忽略
                if (field.isAnnotationPresent(Property.class) && field.getAnnotation(Property.class).isIgnore()) {
                    continue;
                }
                result.add(fields[i].getName());
            }
            return result.toArray(new String[0]);
        }
    }
}



package com.neo.util.modifyinfo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import com.neo.util.ArrayUtils;
import com.neo.util.ReflectUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 组装修改信息工具类
 * <p>
 * 处理器优先级：Property#typeProcessHandleUsing() > BuilderModifyInfoHandle.Builder#registerProcessHandle() > 默认
 * 属性名称优先级：BuilderModifyInfoHandle.Builder#putFieldTitleMapping() > Property#nameAnnotationClass() > Property#name()
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

    private BuilderModifyInfoHandle(Builder builder) {
        setNewObject(builder.newObject);
        setOldObject(builder.oldObject);
        setCompareFields(builder.compareFields);
        setTypeProcessHandleMap(builder.typeProcessHandleMap);
        setOldValueKey(builder.oldValueKey);
        setNewValueKey(builder.newValueKey);
        setFieldNameKey(builder.fieldNameKey);
        setFieldTitleKey(builder.fieldTitleKey);
        setFieldTitleMapping(builder.fieldTitleMapping);
        setIgnoreCompareFields(builder.ignoreCompareFields);
    }


    /**
     * 组装对象变动信息
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:59
     * @Version: 1.0
     */
    public JSONArray builderModifyInfo(Object newObject, Object oldObject, String... compareFields) {
        if (Objects.isNull(newObject) || Objects.isNull(oldObject)) {
            throw new RuntimeException("参数异常");
        }
        JSONArray jsonArray = new JSONArray();
        try {
            for (String f : compareFields) {

                Object newValue = ReflectUtils.getValueByFieldName(newObject, f);
                Object oldValue = ReflectUtils.getValueByFieldName(oldObject, f);
                if (!isDifferent(f, newValue, oldValue)) {
                    continue;
                }
                jsonArray.add(builderModifyInfo(newObject, oldObject, f, newValue, oldValue));
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
    public JSONObject builderModifyInfo(Object newObject, Object oldObject, String fieldName, Object newValue, Object oldValue)
            throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        String title = getFieldTitle(newObject, fieldName);
        if (StringUtils.isBlank(title)) {
            title = getFieldTitle(oldObject, fieldName);
        }

        // 自义定格式化方式
        TypeProcessHandle handle = getTypeProcessHandle(newObject, fieldName, newValue);
        if (Objects.isNull(handle)) {
            throw new RuntimeException("没有找到处理器");
        }

        Object before = handle.format(newValue);
        Object after = handle.format(oldValue);

        JSONObject json = new JSONObject();
        json.put(this.fieldTitleKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(title, StringUtils.EMPTY) : title);
        json.put(this.fieldNameKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(fieldName, StringUtils.EMPTY) : fieldName);
        json.put(this.oldValueKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(before, StringUtils.EMPTY) : before);
        json.put(this.newValueKey, this.isBuilderNullValue ? MoreObjects.firstNonNull(after, StringUtils.EMPTY) : after);
        return json;
    }


    /**
     * 得到类型处理器
     *
     * @Author: Neo
     * @Date: 2019/11/24 17:41
     * @Version: 1.0
     */
    private TypeProcessHandle getTypeProcessHandle(Object object, String fieldName, Object value) throws IllegalAccessException, InstantiationException {
        if (Objects.isNull(object) || StringUtils.isBlank(fieldName)) {
            return null;
        }

        Field field = ReflectUtils.getFieldByFieldName(object.getClass(), fieldName);
        if (Objects.isNull(field)) {
            return null;
        }
        // 获取属性注解处理类
        Property annotation = field.getAnnotation(Property.class);
        if (!Objects.isNull(annotation) && !Objects.isNull(annotation.typeProcessHandleUsing()) && !Void.class.equals(annotation.typeProcessHandleUsing())) {
            if (!TypeProcessHandle.class.isAssignableFrom(annotation.typeProcessHandleUsing())) {
                throw new RuntimeException(Property.class + ".typeProcessHandleUsing:" + annotation.typeProcessHandleUsing() + "不是 TypeProcessHandle 的实现类");
            }
            if (Objects.isNull(annotation.typeProcessHandleUsing())) {
                return null;
            }

            return (TypeProcessHandle) annotation.typeProcessHandleUsing().newInstance();
        }

        // 获取全局处理类
        if (Objects.isNull(this.typeProcessHandleMap) || this.typeProcessHandleMap.isEmpty()) {
            return null;
        }
        TypeProcessHandle handle = this.typeProcessHandleMap.get(value.getClass());

        if (!Objects.isNull(handle)) {
            return handle;
        }

        // 内置处理器
        if (value instanceof BigDecimal) {
            return BIG_DECIMAL_TYPE_PROCESS_HANDLE;
        }
        if (value instanceof Date) {
            return DATE_TYPE_PROCESS_HANDLE;
        }
        return DEFAULT_TYPE_PROCESS_HANDLE;
    }

    /**
     * 得到属性标题
     * <p>
     * 属性名称优先级：BuilderModifyInfoHandle.Builder#putFieldTitleMapping() > Property#nameAnnotationClass() > Property#name()
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

        Property property = field.getAnnotation(Property.class);
        if (Objects.isNull(property)) {
            return null;
        }

        // 2.Property#nameAnnotationClass()
        if (!Objects.isNull(property.nameAnnotationClass()) && StringUtils.isNotBlank(property.nameAnnotationClassField())) {
            // 获取 Property.nameAnnotationClassField 标注的注解属性值
            title = getNameAnnotationTitle(field, property);

            if (StringUtils.isNotBlank(title)) {
                return title;
            }
        }

        // 3.Property#name()
        return property.name();
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
            return StringUtils.EMPTY;
        }
        if (!Annotation.class.isAssignableFrom(property.nameAnnotationClass())) {
            throw new RuntimeException(Property.class + ".nameAnnotationClass:" + property.nameAnnotationClass() + "不是注解类");
        }
        Class<? extends Annotation> nameAnnotationClass = (Class<? extends Annotation>) property.nameAnnotationClass();

        // 获取 Property.nameAnnotationClass 标注的注解代理实例
        Annotation nameAnnotation = field.getAnnotation(nameAnnotationClass);

        //获取代理实例所持有的 InvocationHandler
        InvocationHandler invocationHandler = Proxy.getInvocationHandler(nameAnnotation);

        // 获取 AnnotationInvocationHandler 的 memberValues 字段
        Field memberValuesField = invocationHandler.getClass().getDeclaredField("memberValues");

        // 打开访问权限
        memberValuesField.setAccessible(true);

        // 获取 memberValues
        Map memberValues = (Map) memberValuesField.get(invocationHandler);

        // 获取 Property.nameAnnotationClassField 标注的注解属性值
        String title = (String) memberValues.get(property.nameAnnotationClassField());

        return title;
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
    public boolean isDifferent(String field, Object newValue, Object oldValue) throws IllegalAccessException, InstantiationException {
        // 获取处理器
        TypeProcessHandle handle = getTypeProcessHandle(this.newObject, field, newValue);
        if (Objects.isNull(handle)) {
            throw new RuntimeException("没有找到处理器");
        }

        return handle.isDifferent(newValue, oldValue);
    }

    /**
     * 时间比较
     * <p>
     * 1 - date1 < date2；
     * 0 - date1 = date2；date1 和 date2 都为 null
     * -1 - date1 > date2；
     * -2 - date1、date2其中一个为空
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:20
     * @Version: 1.0
     */
    public static int compareTime(Date date1, Date date2) {
        if (Objects.isNull(date1) && Objects.isNull(date2)) {
            return 0;
        }
        if ((Objects.isNull(date1) && !Objects.isNull(date2)) || (!Objects.isNull(date1) && Objects.isNull(date2))) {
            return -2;
        }
        date1 = clearTimeTail(date1);
        date2 = clearTimeTail(date2);

        return date1.compareTo(date2);
    }


    /**
     * 数字比较
     * <p>
     * 1 - decimal1 < decimal2；
     * 0 - decimal1 = decimal2；decimal1 和 decimal2 都为 null
     * -1 - decimal1 > decimal2；
     * -2 - decimal1、decimal2其中一个为空
     *
     * @Author: Neo
     * @Date: 2019/11/22 22:46
     * @Version: 1.0
     */
    public static int compareBigDecimal(BigDecimal decimal1, BigDecimal decimal2) {
        if (Objects.isNull(decimal1) && Objects.isNull(decimal2)) {
            return 0;
        }

        if ((Objects.isNull(decimal1) && !Objects.isNull(decimal2) || (!Objects.isNull(decimal1) && Objects.isNull(decimal2)))) {
            return -2;
        }

        return decimal1.compareTo(decimal2);
    }

    /**
     * 清除毫秒
     *
     * @Author: Neo
     * @Date: 2019/11/22 21:23
     * @Version: 1.0
     */
    public static Date clearTimeTail(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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

    public static Builder Builder(Object newObject, Object oldObject) {
        Builder builder = new Builder();
        builder.newObject(newObject);
        builder.oldObject(oldObject);
        return builder;
    }

    public static Builder Builder(Object newObject, Object oldObject, String... compareFields) {
        Builder builder = new Builder();
        builder.newObject(newObject);
        builder.oldObject(oldObject);
        builder.compareFields(compareFields);
        return builder;
    }


    public static final class Builder {
        private Object newObject;
        private Object oldObject;
        private String[] compareFields;
        private String[] ignoreCompareFields;
        private Map<Class<?>, TypeProcessHandle> typeProcessHandleMap;

        private String oldValueKey = "before";
        private String newValueKey = "after";
        private String fieldNameKey = "name";
        private String fieldTitleKey = "title";
        private boolean isBuilderNullValue = true;

        private Map<String, String> fieldTitleMapping;

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


        public Builder registerProcessHandle(TypeProcessHandle handle) {
            if (!Objects.isNull(handle)) {
                if (null == typeProcessHandleMap) {
                    typeProcessHandleMap = new HashMap<>();
                }
                typeProcessHandleMap.put(handle.supportTypeKey(), handle);
            }
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


        public JSONArray build() {
            BuilderModifyInfoHandle handle = new BuilderModifyInfoHandle(this);
            String[] compareFields = mergeCompareFields(handle);
            if (ArrayUtils.isEmpty(compareFields)) {
                throw new RuntimeException("没有需要比较的字段");
            }
            return handle.builderModifyInfo(handle.getNewObject(), handle.getOldObject(), compareFields);
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
                return (String[]) result.toArray();
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
            String[] result = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                result[i] = fields[i].getName();
            }
            return result;
        }
    }
}



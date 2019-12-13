package com.neo.util.diffinfo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 组装修改信息属性注解
 *
 * @Author: Neo
 * @Date: 2019/11/23 11:24
 * @Version: 1.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Property {
    /**
     * 名称
     */
    String name() default "";

    /**
     * 类型处理使用类
     */
    Class<?> typeProcessHandleUsing() default Void.class;

    /**
     * 名称标注注解
     * 单独使用无效，需配合{@link #nameAnnotationClassField}
     */
    Class<?> nameAnnotationClass() default Void.class;

    /**
     * 名称标注注解属性
     * 单独使用无效，需配合{@link #nameAnnotationClass}
     */
    String nameAnnotationClassField() default "";

    /**
     * 是否忽略改属性
     */
    boolean isIgnore() default false;
}

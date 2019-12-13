package com.neo.util.diffinfo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内置处理器标志
 * 标注在 {@link TypeProcessHandle} 的实现类上才有效
 * 
 * @Author: Neo
 * @Date: 2019/12/13 21:38
 * @Version: 1.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface InnerTypeProcessHandle {}

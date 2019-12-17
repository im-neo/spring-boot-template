package com.neo.util.diffinfo;

import com.alibaba.fastjson.util.TypeUtils;

import java.util.Objects;


/**
 * 默认类型处理器
 *
 * @Author: Neo
 * @Date: 2019/11/24 17:40
 * @Version: 1.0
 */
public class DefaultTypeProcessHandle implements TypeProcessHandle<Object> {
    @Override
    public boolean isDifferent(Object o1, Object o2) {
        return !Objects.equals(o1, o2);
    }

    @Override
    public Class<?> supportTypeKey() {
        return Object.class;
    }

    @Override
    public Object format(Class<?> clazz, String fieldName, Object value) {
        return TypeUtils.castToString(value);
    }
}

package com.neo.util.modifyinfo;

import com.alibaba.fastjson.util.TypeUtils;
import org.apache.commons.lang3.StringUtils;


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
        String n = TypeUtils.castToString(o1);
        String o = TypeUtils.castToString(o2);
        return !StringUtils.equals(n, o);
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

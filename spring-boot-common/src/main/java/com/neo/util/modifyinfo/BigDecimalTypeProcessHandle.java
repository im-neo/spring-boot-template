package com.neo.util.modifyinfo;

import com.alibaba.fastjson.util.TypeUtils;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 时间类型处理器
 *
 * @Author: Neo
 * @Date: 2019/11/24 17:40
 * @Version: 1.0
 */
public class BigDecimalTypeProcessHandle implements TypeProcessHandle<BigDecimal>{
    @Override
    public boolean isDifferent(Object o1, Object o2) {
        BigDecimal d1 = TypeUtils.castToBigDecimal(o1);
        BigDecimal d2 = TypeUtils.castToBigDecimal(o2);
        return Objects.isNull(d1) ? Objects.isNull(d2) : d1.compareTo(d2) != 0;
    }

    @Override
    public Class<?> supportTypeKey() {
        return BigDecimal.class;
    }

    @Override
    public Object format(Class<?> clazz, String fieldName, Object value) {
        return value;
    }
}

package com.neo.util.modifyinfo;

import com.alibaba.fastjson.util.TypeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * 时间类型处理器
 *
 * @Author: Neo
 * @Date: 2019/11/24 17:40
 * @Version: 1.0
 */
public class DateTypeProcessHandle implements TypeProcessHandle<Date> {

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean isDifferent(Object o1, Object o2) {
        Date d1 = TypeUtils.castToDate(o1);
        Date d2 = TypeUtils.castToDate(o2);

        return Objects.isNull(d1) ? Objects.isNull(d2) : d1.compareTo(d2) != 0;
    }

    @Override
    public Class<?> supportTypeKey() {
        return Date.class;
    }


    @Override
    public Object format(Class<?> clazz, String fieldName, Object value) {
        Date date = TypeUtils.castToDate(value);
        if (Objects.isNull(date)) {
            return null;
        }
        return sdf.format(date);
    }
}

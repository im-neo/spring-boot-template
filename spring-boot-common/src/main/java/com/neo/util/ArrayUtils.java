package com.neo.util;

import java.util.Objects;

/**
 * 数组工具
 *
 * @Author: Neo
 * @Date: 2019/11/24 16:49
 * @Version: 1.0
 */
public class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * 数组长度
     *
     * @Author: Neo
     * @Date: 2019/11/24 16:49
     * @Version: 1.0
     */
    public static int length(Object[] array) {
        return Objects.isNull(array) ? 0 : array.length;
    }

    /**
     * 是否为空数组
     *
     * @Author: Neo
     * @Date: 2019/11/24 17:02
     * @Version: 1.0
     */
    public static boolean isEmpty(Object[] array) {
        return length(array) == 0;
    }

    /**
     * 不否不为空数组
     *
     * @Author: Neo
     * @Date: 2019/11/24 17:02
     * @Version: 1.0
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }
}

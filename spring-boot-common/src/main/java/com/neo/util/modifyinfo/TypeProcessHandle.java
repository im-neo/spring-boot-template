package com.neo.util.modifyinfo;

/**
 * 
 *
 * @Author: Neo
 * @Date: 2019/11/22 23:13
 * @Version: 1.0
 */
public interface TypeProcessHandle<T> {
    
    /**
     * 判断两个值是否不同
     *
     * @Author: Neo
     * @Date: 2019/11/22 23:13
     * @Version: 1.0
     */
    boolean isDifferent(Object o1, Object o2);

    /**
     * 支持的类型
     *
     * @Author: Neo
     * @Date: 2019/11/22 23:14
     * @Version: 1.0
     */
    Class<?> supportTypeKey();
    
    /**
     * 格式化
     *
     * @Author: Neo
     * @Date: 2019/11/23 0:13
     * @Version: 1.0
     */
    Object format(Class<?> clazz , String fieldName , Object value);
}

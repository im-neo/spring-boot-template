package com.neo.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 证件号码生成器
 * 
 * @Author: Neo
 * @Date: 2019/12/26 17:31
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdCardGenerator {

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区县
     */
    private String district;

    /**
     * 年
     */
    private int year;

    /**
     * 月
     */
    private int month;

    /**
     * 日
     */
    private int day;

    /**
     * 性别
     */
    private Gender gender;

    /**
     * 生成个数
     */
    private int count;


    enum Gender {
        男,
        女
    }


}

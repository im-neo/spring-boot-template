package com.neo.util.idcard;

import com.neo.util.Gender;
import com.neo.util.district.DistrictPicker;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;


/**
 * 证件号码生成器
 *
 * @Author: Neo
 * @Date: 2019/12/26 17:31
 * @Version: 1.0
 */
public class IdCardGenerator {
    /**
     * 区县
     */
    private DistrictPicker district;

    /**
     * 区县代码
     */
    private String districtCode;

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




    private IdCardGenerator(Builder builder) {
        setDistrict(builder.district);
        setDistrictCode(builder.districtCode);
        setYear(builder.year);
        setMonth(builder.month);
        setDay(builder.day);
        setGender(builder.gender);
        setCount(builder.count);
    }
    
    
    
    
    
    
    public Set<String> generator(){
        
        return null;
    }


    public void setDistrict(DistrictPicker district) {
        this.district = district;
        this.districtCode = district.code();
    }


    public DistrictPicker getDistrict() {
        return district;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
        if(StringUtils.isNotBlank(districtCode)){
            this.district = DistrictPicker.valueOf(districtCode);
        }
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public static IdCardGenerator.Builder Builder(DistrictPicker district, int year , int count) {
        IdCardGenerator.Builder builder = new IdCardGenerator.Builder();
        builder.district(district);
        builder.year(year);
        builder.count(count);
        return builder;
    }

    public static IdCardGenerator.Builder Builder(String districtCode, int year , int count) {
        IdCardGenerator.Builder builder = new IdCardGenerator.Builder();
        builder.districtCode(districtCode);
        builder.year(year);
        builder.count(count);
        return builder;
    }
    
    public static final class Builder {
        private DistrictPicker district;
        private String districtCode;
        private int year;
        private int month;
        private int day;
        private Gender gender;
        private int count;

        public Builder(){}

        public Builder district(DistrictPicker district) {
            this.district = district;
            return this;
        }

        public Builder districtCode(String districtCode) {
            this.districtCode = districtCode;
            return this;
        }

        public Builder year(int year) {
            this.year = year;
            return this;
        }

        public Builder month(int month) {
            this.month = month;
            return this;
        }

        public Builder day(int day) {
            this.day = day;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public IdCardGenerator build() {
            return new IdCardGenerator(this);
        }
        
        
        public Set<String> generator(){
            IdCardGenerator generator = new IdCardGenerator(this);
            return generator.generator();
        }
    }
}

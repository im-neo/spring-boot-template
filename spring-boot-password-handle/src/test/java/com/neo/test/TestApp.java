package com.neo.test;

import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.base.Stopwatch;
import com.neo.PasswordHandleApplication;
import com.neo.modifyinfo.Property;
import com.neo.modifyinfo.ReflectUtils;
import com.neo.service.PasswordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {PasswordHandleApplication.class})
public class TestApp {

    @Autowired
    private PasswordService passwordService;

    @Test
    public void test01() {
        String filePath = "C:\\Users\\EDZ\\Desktop\\password.txt";
        Stopwatch stopwatch = Stopwatch.createStarted();
        int count = passwordService.loadFromFile(filePath);
        System.out.println("成功导入：" + count + "，耗时：" + stopwatch.elapsed(TimeUnit.SECONDS));
    }
    
    @Test
    public void test02(){
        Stopwatch stopwatch = Stopwatch.createStarted();
        String plaintext = passwordService.forceCrackPassword("e8233f5c2d747116b6395258ce7762f4");
        
        System.out.println("字典破解："+plaintext+"，耗时："+stopwatch.elapsed());
    }
    
    @Test
    public void test03() throws NoSuchFieldException , IllegalAccessException {
        TestBean bean = TestBean.builder()
                .string("String")
                .integer(1)
                .date(TypeUtils.castToDate("2019-11-22 21:29:34"))
                .bigDecimal(TypeUtils.castToBigDecimal("10"))
                .aBoolean(true)
                .build();

        Field field = ReflectUtils.getFieldByFieldName(bean.getClass(), "date");

        Property property = field.getAnnotation(Property.class);

        Class<? extends Annotation> nameAnnotationClass = (Class<? extends Annotation>) property.nameAnnotationClass();

        Annotation nameAnnotation = field.getAnnotation(nameAnnotationClass);

        InvocationHandler invocationHandler = Proxy.getInvocationHandler(nameAnnotation);
        
        Field memberValuesField = invocationHandler.getClass().getDeclaredField("memberValues");
        memberValuesField.setAccessible(true);

        Map map = (Map) memberValuesField.get(invocationHandler);
        
        System.out.println("FieldName :" + map.get(property.nameAnnotationClassField()));

    }
}

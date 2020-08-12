package com.neo.test;

import com.google.common.base.Stopwatch;
import com.neo.PasswordHandleApplication;
import com.neo.service.PasswordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
        String plaintext = passwordService.forceCrackPassword("1f47e5554774c257cb700671782e0bbe");
        
        System.out.println("字典破解："+plaintext+"，耗时："+stopwatch.elapsed());
    }

}

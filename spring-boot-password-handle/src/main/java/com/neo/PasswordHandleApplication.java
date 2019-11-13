package com.neo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.neo")
public class PasswordHandleApplication {
    public static void main(String[] args) {
        SpringApplication.run(PasswordHandleApplication.class, args);
    }
}

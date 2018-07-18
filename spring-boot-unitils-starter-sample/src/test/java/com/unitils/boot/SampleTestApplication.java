package com.unitils.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.unitils.boot", "com.unitils.boot.autoconfigure"})
@MapperScan(basePackages = "com.unitils.boot.mapper")
public class SampleTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleTestApplication.class, args);
    }
}

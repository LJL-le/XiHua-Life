package com.xhulife;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.xhulife.mapper")
@EnableScheduling
@SpringBootApplication
public class XhuLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(XhuLifeApplication.class, args);
    }

}


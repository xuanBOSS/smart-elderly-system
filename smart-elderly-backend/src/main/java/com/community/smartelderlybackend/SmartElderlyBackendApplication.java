package com.community.smartelderlybackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.community.smartelderlybackend.mapper") //
public class SmartElderlyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartElderlyBackendApplication.class, args);
    }

}
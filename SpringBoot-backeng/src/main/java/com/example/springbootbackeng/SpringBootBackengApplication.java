package com.example.springbootbackeng;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 应用启动类
 */
@SpringBootApplication
@EnableKafka
@MapperScan("com.example.springbootbackeng.mapper") // 扫描Mapper接口
public class SpringBootBackengApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootBackengApplication.class, args);
    }
}
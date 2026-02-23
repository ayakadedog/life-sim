package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用程序启动入口
 * 包含 Spring Boot 应用的 main 方法
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * 启动 Spring Boot 应用程序
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}

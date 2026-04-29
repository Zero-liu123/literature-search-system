package com.example.literaturesearchsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LiteratureSearchSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteratureSearchSystemApplication.class, args);
        // syncAllToEs() 已在 SearchServiceImpl 的 @PostConstruct 中自动执行，无需重复调用
    }
}

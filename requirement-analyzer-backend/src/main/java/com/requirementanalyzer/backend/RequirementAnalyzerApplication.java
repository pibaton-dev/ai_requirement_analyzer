package com.requirementanalyzer.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RequirementAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequirementAnalyzerApplication.class, args);
    }
}
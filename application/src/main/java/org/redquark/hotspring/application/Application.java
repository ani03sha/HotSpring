package org.redquark.hotspring.application;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.redquark.hotspring"})
@OpenAPIDefinition(
        info = @Info(
                title = "Hot Spring",
                description = "A bunch of \"hawwwt\" Spring framework based demos.",
                version = "0.0.1-SNAPSHOT"
        )
)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

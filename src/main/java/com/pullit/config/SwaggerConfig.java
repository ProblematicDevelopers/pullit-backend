package com.pullit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
        
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");
        
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local server");
        
        Server prodServer = new Server()
                .url("http://3.35.47.6")
                .description("Production server");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Pullit API")
                        .version("1.0.0")
                        .description("문제은행 사이트 Pullit의 REST API 문서입니다.")
                        .contact(new Contact()
                                .name("Pullit Team")
                                .email("team@pullit.com")))
                .servers(List.of(localServer, prodServer))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .security(List.of(securityRequirement));
    }
}
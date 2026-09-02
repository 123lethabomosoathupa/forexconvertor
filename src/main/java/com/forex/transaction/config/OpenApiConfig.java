package com.forex.transaction.config;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Forex Transaction Service API")
                        .version("1.0.0")
                        .description("Records and queries forex conversion history. Backed by MongoDB."))
                .servers(List.of(
                        new Server().url("https://forexconvertor2.onrender.com").description("Production (Render)"),
                        new Server().url("http://localhost:8082").description("Local dev")));
    }
}

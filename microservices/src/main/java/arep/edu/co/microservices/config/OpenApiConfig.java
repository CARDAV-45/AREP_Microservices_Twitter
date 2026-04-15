package arep.edu.co.microservices.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Twitter-like API",
                version = "1.0",
                description = "API REST de una aplicación similar a Twitter con autenticación Auth0. " +
                              "Los endpoints marcados con el candado requieren un JWT Bearer token válido.",
                contact = @Contact(name = "AREP Group", email = "group@arep.edu.co")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor local")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingresa el token JWT de Auth0. Formato: Bearer {token}"
)
public class OpenApiConfig {
}

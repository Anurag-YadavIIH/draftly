package com.airtribe.draftly.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Swagger UI metadata. Once the app is running, the interactive
 * API explorer is available at http://localhost:8080/swagger-ui.html which is
 * very handy for the demo video.
 *
 * Registers a "bearerAuth" scheme so the UI shows an "Authorize" button -
 * paste the JWT returned by {@code POST /api/auth/login} (or
 * {@code /register}) there to call the protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI draftlyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Draftly - Gmail AI Reply Agent API")
                        .description("Backend that fetches emails, generates AI reply drafts (with RAG style-matching), "
                                + "lets users review/approve, and sends approved replies via Gmail.")
                        .version("1.0.0")
                        .contact(new Contact().name("Airtribe Backend Launchpad Capstone")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}

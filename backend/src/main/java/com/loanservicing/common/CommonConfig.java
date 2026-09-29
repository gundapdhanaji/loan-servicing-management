package com.loanservicing.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Shared beans used by every module. */
@Configuration
public class CommonConfig {

    @Bean
    public AppClock appClock() {
        return new AppClock(Clock.systemDefaultZone());
    }

    /**
     * Swagger UI "Authorize" button: paste the token from /api/v1/auth/get_auth_token.
     * Swagger then sends it in the "jwt" header, exactly like the LoanLinq React app.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Loan Servicing Management API").version("v1"))
                .components(new Components().addSecuritySchemes("jwt",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("jwt")))
                .addSecurityItem(new SecurityRequirement().addList("jwt"));
    }
}

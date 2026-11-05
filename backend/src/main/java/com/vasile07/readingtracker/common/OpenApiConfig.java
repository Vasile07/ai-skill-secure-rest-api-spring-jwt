package com.vasile07.readingtracker.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(info = @Info(title = "Reading Tracker API", version = "1.0",
        description = "Register or log in through /auth to receive tokens. Use the access token as a Bearer credential for protected endpoints. The ID token is for client identity display, and the refresh token is only for refresh/logout."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig { }

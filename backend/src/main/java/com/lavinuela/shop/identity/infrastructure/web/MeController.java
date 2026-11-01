package com.lavinuela.shop.identity.infrastructure.web;

import com.lavinuela.shop.identity.application.CurrentUserService;
import com.lavinuela.shop.identity.application.UserSummary;
import com.lavinuela.shop.shared.infrastructure.web.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La persona de la sesión actual. Primera operación protegida de la API: exige un token de acceso válido. */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Cuentas")
class MeController {

    private final CurrentUserService currentUser;

    MeController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(operationId = "getCurrentUser", summary = "Quién soy",
            description = "Devuelve los datos de la persona que ha iniciado sesión.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "La persona de la sesión",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión o con el token de acceso caducado (AUTH_REQUIRED, AUTH_TOKEN_EXPIRED)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemResponse.class)))
    UserResponse me(Authentication authentication) {
        UserSummary user = currentUser.currentUser(authentication.getName());
        return new UserResponse(user.id(), user.username(), user.email(), user.roles());
    }
}

package arep.edu.co.microservices.controller;

import arep.edu.co.microservices.dto.UserResponse;
import arep.edu.co.microservices.model.User;
import arep.edu.co.microservices.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Usuario", description = "Información del usuario autenticado")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Obtener perfil del usuario autenticado",
            description = "Retorna los datos del usuario autenticado. Si es su primer acceso, crea el perfil automáticamente.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}

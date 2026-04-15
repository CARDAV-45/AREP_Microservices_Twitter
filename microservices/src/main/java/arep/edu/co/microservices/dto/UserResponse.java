package arep.edu.co.microservices.dto;

import arep.edu.co.microservices.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Respuesta con los datos del usuario autenticado")
public record UserResponse(

        @Schema(description = "ID del usuario (Auth0 sub)") String id,
        @Schema(description = "Email del usuario") String email,
        @Schema(description = "Nombre del usuario") String name,
        @Schema(description = "Nickname del usuario") String nickname,
        @Schema(description = "URL de la foto de perfil") String picture,
        @Schema(description = "Fecha de creación de la cuenta") LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPicture(),
                user.getCreatedAt()
        );
    }
}

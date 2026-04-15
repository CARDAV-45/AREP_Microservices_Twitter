package arep.edu.co.microservices.dto;

import arep.edu.co.microservices.model.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Respuesta con los datos de un post")
public record PostResponse(

        @Schema(description = "ID del post") Long id,
        @Schema(description = "Contenido del post") String content,
        @Schema(description = "Fecha y hora de creación") LocalDateTime createdAt,
        @Schema(description = "ID del autor (Auth0 sub)") String authorId,
        @Schema(description = "Nombre del autor") String authorName,
        @Schema(description = "Foto de perfil del autor") String authorPicture
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getCreatedAt(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getAuthor().getPicture()
        );
    }
}

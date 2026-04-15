package arep.edu.co.microservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear un nuevo post")
public record PostRequest(

        @Schema(description = "Contenido del post (máximo 140 caracteres)", example = "Hola mundo desde mi primer tweet!")
        @NotBlank(message = "El contenido no puede estar vacío")
        @Size(max = 140, message = "El contenido no puede superar los 140 caracteres")
        String content
) {}

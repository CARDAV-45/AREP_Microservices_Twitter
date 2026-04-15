package arep.edu.co.microservices.controller;

import arep.edu.co.microservices.dto.PostRequest;
import arep.edu.co.microservices.dto.PostResponse;
import arep.edu.co.microservices.model.User;
import arep.edu.co.microservices.service.PostService;
import arep.edu.co.microservices.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Operaciones del stream público de posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Obtener stream público",
            description = "Retorna todos los posts ordenados del más reciente al más antiguo. No requiere autenticación."
    )
    public ResponseEntity<List<PostResponse>> getStream() {
        return ResponseEntity.ok(postService.getStream());
    }

    @PostMapping
    @Operation(
            summary = "Crear un post",
            description = "Crea un nuevo post de hasta 140 caracteres. Requiere JWT válido de Auth0.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User author = userService.getOrCreateUser(jwt);
        PostResponse response = postService.createPost(request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

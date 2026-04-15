package arep.edu.co.microservices.service;

import arep.edu.co.microservices.model.User;
import arep.edu.co.microservices.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        String sub = jwt.getSubject();

        return userRepository.findById(sub).orElseGet(() -> {
            User user = User.builder()
                    .id(sub)
                    .email(getClaimOrDefault(jwt, "email", sub + "@unknown.com"))
                    .name(getClaimOrDefault(jwt, "name", "Usuario"))
                    .nickname(getClaimOrDefault(jwt, "nickname", sub))
                    .picture(getClaimOrDefault(jwt, "picture", null))
                    .build();
            return userRepository.save(user);
        });
    }

    private String getClaimOrDefault(Jwt jwt, String claim, String defaultValue) {
        // Primero busca el claim estándar, luego el claim con namespace de Auth0
        String value = jwt.getClaimAsString(claim);
        if (value == null || value.isBlank()) {
            value = jwt.getClaimAsString("https://twitter-api/" + claim);
        }
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}

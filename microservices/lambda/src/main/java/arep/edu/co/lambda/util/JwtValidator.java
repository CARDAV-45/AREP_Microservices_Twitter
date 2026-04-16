package arep.edu.co.lambda.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class JwtValidator {

    private static final String DOMAIN   = System.getenv("AUTH0_DOMAIN");
    private static final String AUDIENCE = System.getenv("AUTH0_AUDIENCE");

    private static final JWKSource<SecurityContext> JWK_SOURCE;

    static {
        try {
            JWK_SOURCE = new RemoteJWKSet<>(new URL("https://" + DOMAIN + "/.well-known/jwks.json"));
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando JWKS", e);
        }
    }

    public static JWTClaimsSet validate(String bearerToken) throws Exception {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new Exception("Token ausente o mal formado");
        }
        String token = bearerToken.substring(7);

        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, JWK_SOURCE));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().issuer("https://" + DOMAIN + "/").build(),
                new HashSet<>(Arrays.asList("sub", "iss", "exp", "iat"))
        ));

        JWTClaimsSet claims = processor.process(token, null);
        List<String> aud = claims.getAudience();
        if (aud == null || !aud.contains(AUDIENCE)) {
            throw new Exception("Audience inválido");
        }
        return claims;
    }
}

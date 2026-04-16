package arep.edu.co.lambda;

import arep.edu.co.lambda.util.JwtValidator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.nimbusds.jwt.JWTClaimsSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "twitter-users";
    private static final String NS    = "https://twitter-api/";
    private static final Gson   GSON  = new Gson();

    private final DynamoDbClient dynamo = DynamoDbClient.builder()
            .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        Map<String, String> cors = corsHeaders();
        try {
            String authHeader = getHeader(event, "Authorization");
            JWTClaimsSet claims = JwtValidator.validate(authHeader);

            String sub      = claims.getSubject();
            String name     = getClaim(claims, NS + "name",     sub);
            String email    = getClaim(claims, NS + "email",    sub + "@unknown.com");
            String nickname = getClaim(claims, NS + "nickname", sub);
            String picture  = getClaim(claims, NS + "picture",  "");

            // Insertar solo si no existe
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id",        attr(sub));
            item.put("name",      attr(name));
            item.put("email",     attr(email));
            item.put("nickname",  attr(nickname));
            item.put("picture",   attr(picture));
            item.put("createdAt", attr(Instant.now().toString()));

            try {
                dynamo.putItem(PutItemRequest.builder()
                        .tableName(TABLE).item(item)
                        .conditionExpression("attribute_not_exists(id)")
                        .build());
            } catch (ConditionalCheckFailedException ignored) {
                // usuario ya existe, no pasa nada
            }

            GetItemResponse res = dynamo.getItem(GetItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("id", attr(sub)))
                    .build());

            Map<String, Object> user = new HashMap<>();
            res.item().forEach((k, v) -> user.put(k, v.s()));

            return ok(GSON.toJson(user), cors);

        } catch (Exception e) {
            int status = e.getMessage().contains("Token") || e.getMessage().contains("Audience") ? 401 : 500;
            return error(status, e.getMessage(), cors);
        }
    }

    private String getHeader(APIGatewayProxyRequestEvent event, String key) {
        if (event.getHeaders() == null) return null;
        String v = event.getHeaders().get(key);
        return v != null ? v : event.getHeaders().get(key.toLowerCase());
    }

    private String getClaim(JWTClaimsSet claims, String key, String fallback) {
        try {
            Object v = claims.getClaim(key);
            return (v != null && !v.toString().isBlank()) ? v.toString() : fallback;
        } catch (Exception e) { return fallback; }
    }

    private AttributeValue attr(String v) {
        return AttributeValue.builder().s(v != null ? v : "").build();
    }

    private APIGatewayProxyResponseEvent ok(String body, Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(body);
    }

    private APIGatewayProxyResponseEvent error(int code, String msg, Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent().withStatusCode(code).withHeaders(headers)
                .withBody("{\"error\":\"" + msg + "\"}");
    }

    private Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin",  "*");
        h.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        h.put("Access-Control-Allow-Methods", "GET,OPTIONS");
        h.put("Content-Type", "application/json");
        return h;
    }
}

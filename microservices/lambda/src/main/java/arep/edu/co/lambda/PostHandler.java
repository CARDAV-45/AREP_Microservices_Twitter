package arep.edu.co.lambda;

import arep.edu.co.lambda.util.JwtValidator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "twitter-posts";
    private static final String NS    = "https://twitter-api/";
    private static final Gson   GSON  = new Gson();

    private final DynamoDbClient dynamo = DynamoDbClient.builder()
            .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        Map<String, String> cors = corsHeaders();

        if ("OPTIONS".equalsIgnoreCase(event.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(cors).withBody("");
        }

        try {
            String authHeader = getHeader(event, "Authorization");
            JWTClaimsSet claims = JwtValidator.validate(authHeader);

            if (event.getBody() == null || event.getBody().isBlank()) {
                return error(400, "Body requerido", cors);
            }

            JsonObject body    = GSON.fromJson(event.getBody(), JsonObject.class);
            String content     = body.has("content") ? body.get("content").getAsString() : "";

            if (content.isBlank())       return error(400, "El contenido no puede estar vacío", cors);
            if (content.length() > 140)  return error(400, "Máximo 140 caracteres", cors);

            String sub      = claims.getSubject();
            String name     = getClaim(claims, NS + "name",    sub);
            String picture  = getClaim(claims, NS + "picture", "");
            String postId   = UUID.randomUUID().toString();
            String createdAt= Instant.now().toString();

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id",            attr(postId));
            item.put("content",       attr(content));
            item.put("authorId",      attr(sub));
            item.put("authorName",    attr(name));
            item.put("authorPicture", attr(picture));
            item.put("createdAt",     attr(createdAt));

            dynamo.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());

            Map<String, Object> response = new HashMap<>();
            response.put("id",            postId);
            response.put("content",       content);
            response.put("authorId",      sub);
            response.put("authorName",    name);
            response.put("authorPicture", picture);
            response.put("createdAt",     createdAt);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201).withHeaders(cors).withBody(GSON.toJson(response));

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

    private APIGatewayProxyResponseEvent error(int code, String msg, Map<String, String> headers) {
        return new APIGatewayProxyResponseEvent().withStatusCode(code).withHeaders(headers)
                .withBody("{\"error\":\"" + msg + "\"}");
    }

    private Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin",  "*");
        h.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        h.put("Access-Control-Allow-Methods", "POST,OPTIONS");
        h.put("Content-Type", "application/json");
        return h;
    }
}

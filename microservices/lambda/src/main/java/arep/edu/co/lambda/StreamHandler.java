package arep.edu.co.lambda;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;


public class StreamHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE = "twitter-posts";
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
            ScanResponse scan = dynamo.scan(ScanRequest.builder().tableName(TABLE).build());

            List<Map<String, String>> posts = scan.items().stream()
                    .map(item -> {
                        Map<String, String> post = new HashMap<>();
                        item.forEach((k, v) -> post.put(k, v.s()));
                        return post;
                    })
                    .sorted(Comparator.comparing(
                            (Map<String, String> p) -> p.getOrDefault("createdAt", ""),
                            Comparator.reverseOrder()
                    ))
                    .collect(Collectors.toList());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200).withHeaders(cors).withBody(GSON.toJson(posts));

        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500).withHeaders(cors)
                    .withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
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

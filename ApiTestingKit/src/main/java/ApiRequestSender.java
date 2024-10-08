import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiRequestSender {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Track request start times and completion status by correlation ID
    private static final ConcurrentHashMap<String, Instant> requestTimeMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> requestCompletedMap = new ConcurrentHashMap<>();

    // List to store test results for presentation
    private static final List<Map<String, Object>> resultsList = Collections.synchronizedList(new ArrayList<>());

    // Duration for the API test (e.g., 2 hours)
    private static final long TEST_DURATION_HOURS = 2;
    private static final long TEST_DURATION_SECONDS = TEST_DURATION_HOURS * 3600;

    // Port for intercepting the second API
    private static final int INTERCEPTOR_PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Start the interceptor server
        startInterceptorServer();

        // ScheduledExecutorService to schedule tasks at fixed intervals
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Track the start time of the entire test
        Instant testStartTime = Instant.now();

        // Schedule the task to run every second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check if test duration has exceeded the limit (2 hours)
                if (Duration.between(testStartTime, Instant.now()).getSeconds() > TEST_DURATION_SECONDS) {
                    scheduler.shutdown();
                    System.out.println("Test completed. No more requests will be sent.");
                    // Show final results
                    displayTestResults();
                } else {
                    // Process an individual API request
                    processRequest();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);  // Delay = 0, Period = 1 second (1 request per second)
    }

    private static void processRequest() throws Exception {
        // Load JSON from file
        File jsonFile = new File("request-data.json");
        JsonNode jsonData = objectMapper.readTree(jsonFile);

        // Extract headers and body from JSON
        ObjectNode headersNode = (ObjectNode) jsonData.get("headers");
        ObjectNode bodyNode = (ObjectNode) jsonData.get("body");

        // Generate a unique correlation ID
        String correlationId = generateCorrelationId();
        System.out.println("Generated Correlation ID: " + correlationId);

        // Add the correlation ID to the body or headers (based on your API structure)
        headersNode.put("X-Correlation-ID", correlationId); // Add to headers
        bodyNode.put("correlationId", correlationId); // Add to body if needed

        // Convert the body to a string (it will be sent as the body of the request)
        String requestBody = bodyNode.toString();

        // Record the start time and add to map
        Instant startTime = Instant.now();
        requestTimeMap.put(correlationId, startTime);
        requestCompletedMap.put(correlationId, false);

        // Send the primary POST request with headers and body
        sendPostRequest("https://example.com/api/login", headersNode, requestBody, correlationId);
    }

    // Method to send POST request
    private static void sendPostRequest(String url, ObjectNode headers, String requestBody, String correlationId) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

        // Add headers from the JSON file
        Iterator<Map.Entry<String, JsonNode>> headersIterator = headers.fields();
        while (headersIterator.hasNext()) {
            Map.Entry<String, JsonNode> header = headersIterator.next();
            requestBuilder.header(header.getKey(), header.getValue().asText());
        }

        // Build and send the request
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Output response details
        System.out.println("First API Response Status: " + response.statusCode());
        System.out.println("First API Response Body: " + response.body());

        // Handle the completion of the first API request response
        handleResponse(correlationId, "First API");
    }

    // Start an interceptor server to capture the second POST request
    private static void startInterceptorServer() throws IOException {
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress(INTERCEPTOR_PORT), 0);
        server.createContext("/intercept", new InterceptHandler());
        server.setExecutor(null); // Creates a default executor
        server.start();
        System.out.println("Interceptor server started on port " + INTERCEPTOR_PORT);
    }

    // Handler for intercepted requests
    private static class InterceptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle only POST requests
            if ("POST".equals(exchange.getRequestMethod())) {
                // Get correlation ID from the request headers
                String correlationId = exchange.getRequestHeaders().getFirst("X-Correlation-ID");

                // Read the request body
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Intercepted Second API Request Body: " + requestBody);
                System.out.println("Intercepted request with Correlation ID: " + correlationId);

                // Prepare to forward the request
                HttpRequest.Builder forwardRequestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create("http://original-api-endpoint.com")) // Replace with the actual endpoint
                        .method("POST", HttpRequest.BodyPublishers.ofString(requestBody));

                // Forward all original headers
                exchange.getRequestHeaders().forEach((key, values) -> {
                    for (String value : values) {
                        forwardRequestBuilder.header(key, value);
                    }
                });

                // Create the forward request
                HttpRequest forwardRequest = forwardRequestBuilder.build();

                // Send the forwarded request
                try {
                    HttpResponse<String> response = client.send(forwardRequest, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Forwarded request response status: " + response.statusCode());
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error forwarding request: " + e.getMessage());
                }

                // Optionally, you can log the response from the forwarded request
            } else {
                // Handle non-POST requests if needed
                System.out.println("Received non-POST request: " + exchange.getRequestMethod());
            }

            // Close the exchange
            exchange.close();
        }
    }



    // Method to generate a unique correlation ID
    private static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // Handle the completion of a request and log the time taken
    private static void handleResponse(String correlationId, String apiType) {
        if (requestTimeMap.containsKey(correlationId)) {
            Instant startTime = requestTimeMap.get(correlationId);
            Instant endTime = Instant.now();

            // Calculate the time taken for the full flow
            Duration duration = Duration.between(startTime, endTime);
            System.out.println("Time taken for " + apiType + " (Correlation ID: " + correlationId + "): " + duration.toMillis() + " ms");

            // Mark as completed
            requestCompletedMap.put(correlationId, true);
        }
    }

    // Display test results at the end
    private static void displayTestResults() {
        System.out.println("\n--- Test Results ---");
        for (String correlationId : requestTimeMap.keySet()) {
            Instant startTime = requestTimeMap.get(correlationId);
            Boolean isCompleted = requestCompletedMap.get(correlationId);
            Duration duration = isCompleted ? Duration.between(startTime, Instant.now()) : Duration.ZERO;
            System.out.println("Correlation ID: " + correlationId + " | Time Taken: " + duration.toMillis() + " ms | Completed: " + isCompleted);
        }
    }
}

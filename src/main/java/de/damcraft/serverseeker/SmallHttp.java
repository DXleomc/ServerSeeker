package de.damcraft.serverseeker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static de.damcraft.serverseeker.ServerSeeker.LOG;

public final class SmallHttp {
    // Configuration constants
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "ServerSeeker/1.0";
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
        .connectTimeout(DEFAULT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private SmallHttp() {} // Prevent instantiation

    // Synchronous POST with enhanced options
    public static String post(String url, String json) {
        return post(url, json, DEFAULT_TIMEOUT);
    }

    public static String post(String url, String json, Duration timeout) {
        HttpRequest request = buildPostRequest(url, json, timeout);
        return sendRequest(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .map(HttpResponse::body)
            .orElse(null);
    }

    // Asynchronous POST
    public static CompletableFuture<String> postAsync(String url, String json) {
        return postAsync(url, json, DEFAULT_TIMEOUT);
    }

    public static CompletableFuture<String> postAsync(String url, String json, Duration timeout) {
        HttpRequest request = buildPostRequest(url, json, timeout);
        return SHARED_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(HttpResponse::body)
            .exceptionally(e -> {
                LOG.error("Async POST failed: " + e.getMessage());
                return null;
            });
    }

    // Synchronous GET with enhanced options
    public static String get(String url) {
        return get(url, DEFAULT_TIMEOUT);
    }

    public static String get(String url, Duration timeout) {
        HttpRequest request = buildGetRequest(url, timeout);
        return sendRequest(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .map(HttpResponse::body)
            .orElse(null);
    }

    // Asynchronous GET
    public static CompletableFuture<String> getAsync(String url) {
        return getAsync(url, DEFAULT_TIMEOUT);
    }

    public static CompletableFuture<String> getAsync(String url, Duration timeout) {
        HttpRequest request = buildGetRequest(url, timeout);
        return SHARED_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(HttpResponse::body)
            .exceptionally(e -> {
                LOG.error("Async GET failed: " + e.getMessage());
                return null;
            });
    }

    // Download with progress tracking
    public static HttpResponse<InputStream> download(String url) {
        return download(url, DEFAULT_TIMEOUT, null);
    }

    public static HttpResponse<InputStream> download(String url, Duration timeout, Consumer<Long> progressCallback) {
        HttpRequest request = buildGetRequest(url, timeout);
        Optional<HttpResponse<InputStream>> response = sendRequest(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.isEmpty()) return null;

        // Handle redirects
        if (response.get().headers().firstValue("location").isPresent()) {
            return download(response.get().headers().firstValue("location").get(), timeout, progressCallback);
        }

        return response.get();
    }

    // Common request building methods
    private static HttpRequest buildPostRequest(String url, String json, Duration timeout) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    }

    private static HttpRequest buildGetRequest(String url, Duration timeout) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
    }

    // Common request sending method with proper error handling
    private static <T> Optional<HttpResponse<T>> sendRequest(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return Optional.of(SHARED_CLIENT.send(request, handler));
        } catch (IOException e) {
            LOG.error("I/O error during HTTP request: " + e.getMessage());
        } catch (InterruptedException e) {
            LOG.error("HTTP request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid URL or request parameters: " + e.getMessage());
        } catch (SecurityException e) {
            LOG.error("Security violation during HTTP request: " + e.getMessage());
        }
        return Optional.empty();
    }
}

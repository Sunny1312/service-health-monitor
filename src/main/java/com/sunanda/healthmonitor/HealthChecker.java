package com.sunanda.healthmonitor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/** Sends one HTTP request to a service and turns the outcome into a CheckResult. */
public class HealthChecker {

    private final HttpClient client;
    private final Duration timeout;

    public HealthChecker(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CheckResult check(Service service) {
        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(service.url()))
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<Void> response =
                    client.send(request, HttpResponse.BodyHandlers.discarding());
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.statusCode();
            // 2xx and 3xx mean healthy. 4xx and 5xx mean something is wrong.
            boolean up = status >= 200 && status < 400;
            return new CheckResult(service.name(), service.url(), startedAt, status,
                    latencyMs, up, up ? null : "HTTP " + status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure(service, startedAt, startNanos, "Interrupted");
        } catch (Exception e) {
            // Timeouts, DNS errors, refused connections, bad URLs all land here.
            return failure(service, startedAt, startNanos,
                    e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        }
    }

    private CheckResult failure(Service s, Instant startedAt, long startNanos, String error) {
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        return new CheckResult(s.name(), s.url(), startedAt, -1, latencyMs, false, error);
    }
}

package com.sunanda.healthmonitor;

import java.time.Instant;

/** The outcome of ONE health check of ONE service. */
public record CheckResult(
        String serviceName,
        String url,
        Instant checkedAt,
        int statusCode,      // HTTP status, or -1 if no response (timeout, DNS failure...)
        long latencyMs,      // how long the request took
        boolean up,          // true if the service looked healthy
        String error) {      // error message if it failed, otherwise null
}

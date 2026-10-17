package com.sunanda.healthmonitor;

/** Local mode: just print each result. Used when no DynamoDB table is configured. */
public class ConsoleResultStore implements ResultStore {
    @Override
    public void save(CheckResult r) {
        System.out.printf("[%s] %-12s %s  status=%d  latency=%dms%s%n",
                r.checkedAt(), r.serviceName(), r.up() ? "UP  " : "DOWN",
                r.statusCode(), r.latencyMs(),
                r.error() == null ? "" : "  error=" + r.error());
    }
}

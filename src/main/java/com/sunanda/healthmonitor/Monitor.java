package com.sunanda.healthmonitor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Runs a health check for every service on a repeating schedule. */
public class Monitor {

    private final List<Service> services;
    private final HealthChecker checker;
    private final StatusTracker tracker;
    private final ResultStore store;
    private final Alerter alerter;
    private final int intervalSeconds;
    private final ScheduledExecutorService scheduler;

    public Monitor(List<Service> services, HealthChecker checker, StatusTracker tracker,
                   ResultStore store, Alerter alerter, int intervalSeconds) {
        this.services = services;
        this.checker = checker;
        this.tracker = tracker;
        this.store = store;
        this.alerter = alerter;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newScheduledThreadPool(Math.max(2, services.size()));
    }

    public void start() {
        for (Service service : services) {
            scheduler.scheduleAtFixedRate(() -> runCheck(service),
                    0, intervalSeconds, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /** One full cycle for one service: check, save, and alert if the state changed. */
    void runCheck(Service service) {
        // Catch everything: an uncaught exception would silently stop this schedule.
        try {
            CheckResult result = checker.check(service);
            saveQuietly(result);
            switch (tracker.record(result)) {
                case WENT_DOWN -> alerter.send(
                        "DOWN: " + service.name(),
                        service.name() + " (" + service.url() + ") failed its last checks.\n"
                                + "Last error: " + result.error() + "\nTime: " + result.checkedAt());
                case RECOVERED -> alerter.send(
                        "RECOVERED: " + service.name(),
                        service.name() + " (" + service.url() + ") is back up.\n"
                                + "Latency: " + result.latencyMs() + " ms\nTime: " + result.checkedAt());
                case NONE -> { }
            }
        } catch (Exception e) {
            System.err.println("Error while checking " + service.name() + ": " + e);
        }
    }

    private void saveQuietly(CheckResult result) {
        try {
            store.save(result);
        } catch (Exception e) {
            // A storage problem should not stop monitoring or alerting.
            System.err.println("Could not save result for " + result.serviceName() + ": " + e);
        }
    }
}

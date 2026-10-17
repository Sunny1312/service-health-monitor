package com.sunanda.healthmonitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Decides when a service is really DOWN or has RECOVERED.
 *
 * One failed check can be a blip, so we only declare "down" after N failures
 * IN A ROW (the threshold). This avoids false alarms. We also only report the
 * moment the state CHANGES, so you get one alert, not one every minute.
 */
public class StatusTracker {

    public enum Transition { NONE, WENT_DOWN, RECOVERED }

    private final int failureThreshold;
    private final Map<String, Integer> consecutiveFailures = new HashMap<>();
    private final Map<String, Boolean> markedDown = new HashMap<>();

    public StatusTracker(int failureThreshold) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be at least 1");
        }
        this.failureThreshold = failureThreshold;
    }

    /** Feed in each check result; get back whether the service's state just changed. */
    public synchronized Transition record(CheckResult result) {
        String name = result.serviceName();
        boolean isDown = markedDown.getOrDefault(name, false);

        if (result.up()) {
            consecutiveFailures.put(name, 0);
            if (isDown) {
                markedDown.put(name, false);
                return Transition.RECOVERED;
            }
            return Transition.NONE;
        }

        int failures = consecutiveFailures.merge(name, 1, Integer::sum);
        if (failures >= failureThreshold && !isDown) {
            markedDown.put(name, true);
            return Transition.WENT_DOWN;
        }
        return Transition.NONE;
    }

    public synchronized int failuresFor(String serviceName) {
        return consecutiveFailures.getOrDefault(serviceName, 0);
    }
}

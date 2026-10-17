package com.sunanda.healthmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sunanda.healthmonitor.StatusTracker.Transition;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StatusTrackerTest {

    private static CheckResult result(String name, boolean up) {
        return new CheckResult(name, "http://x", Instant.now(), up ? 200 : 500, 10, up,
                up ? null : "HTTP 500");
    }

    @Test
    void singleFailureDoesNotTriggerDown() {
        StatusTracker tracker = new StatusTracker(3);
        assertEquals(Transition.NONE, tracker.record(result("api", false)));
        assertEquals(1, tracker.failuresFor("api"));
    }

    @Test
    void goesDownAfterThresholdFailuresInARow() {
        StatusTracker tracker = new StatusTracker(3);
        tracker.record(result("api", false));
        tracker.record(result("api", false));
        assertEquals(Transition.WENT_DOWN, tracker.record(result("api", false)));
    }

    @Test
    void alertsOnlyOnceWhileStillDown() {
        StatusTracker tracker = new StatusTracker(2);
        tracker.record(result("api", false));
        assertEquals(Transition.WENT_DOWN, tracker.record(result("api", false)));
        assertEquals(Transition.NONE, tracker.record(result("api", false)));
        assertEquals(Transition.NONE, tracker.record(result("api", false)));
    }

    @Test
    void successInTheMiddleResetsTheCount() {
        StatusTracker tracker = new StatusTracker(3);
        tracker.record(result("api", false));
        tracker.record(result("api", false));
        tracker.record(result("api", true));
        assertEquals(0, tracker.failuresFor("api"));
        assertEquals(Transition.NONE, tracker.record(result("api", false)));
    }

    @Test
    void recoversAfterBeingDown() {
        StatusTracker tracker = new StatusTracker(1);
        assertEquals(Transition.WENT_DOWN, tracker.record(result("api", false)));
        assertEquals(Transition.RECOVERED, tracker.record(result("api", true)));
        assertEquals(Transition.NONE, tracker.record(result("api", true)));
    }

    @Test
    void servicesAreTrackedIndependently() {
        StatusTracker tracker = new StatusTracker(2);
        tracker.record(result("a", false));
        tracker.record(result("b", true));
        assertEquals(Transition.WENT_DOWN, tracker.record(result("a", false)));
        assertEquals(Transition.NONE, tracker.record(result("b", true)));
    }
}

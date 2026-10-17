package com.sunanda.healthmonitor;

/** Local mode: print alerts. Used when no SNS topic is configured. */
public class ConsoleAlerter implements Alerter {
    @Override
    public void send(String subject, String message) {
        System.out.println("*** ALERT: " + subject + " ***");
        System.out.println(message);
    }
}

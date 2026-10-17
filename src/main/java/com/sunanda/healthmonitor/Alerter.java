package com.sunanda.healthmonitor;

/** Something that can notify a human. Implemented for the console and for AWS SNS. */
public interface Alerter {
    void send(String subject, String message);
}

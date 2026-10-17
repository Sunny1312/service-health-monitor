package com.sunanda.healthmonitor;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Sends alerts through an AWS SNS topic (which can email you). */
public class SnsAlerter implements Alerter {

    private final SnsClient client;
    private final String topicArn;

    public SnsAlerter(SnsClient client, String topicArn) {
        this.client = client;
        this.topicArn = topicArn;
    }

    @Override
    public void send(String subject, String message) {
        // SNS limits the subject to 100 characters.
        String safeSubject = subject.length() > 100 ? subject.substring(0, 100) : subject;
        client.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .subject(safeSubject)
                .message(message)
                .build());
    }
}

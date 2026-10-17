package com.sunanda.healthmonitor;

import java.time.Duration;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Entry point.  Usage:  java -jar health-monitor.jar [path/to/services.properties]
 *
 * Optional environment variables:
 *   DYNAMODB_TABLE          save results to this DynamoDB table (otherwise: print to console)
 *   SNS_TOPIC_ARN           send alerts to this SNS topic       (otherwise: print to console)
 *   CHECK_INTERVAL_SECONDS  how often to check (default 60)
 *   FAILURE_THRESHOLD       failures in a row before "DOWN" (default 3)
 * AWS region and credentials come from the standard AWS environment/role.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 0 ? args[0] : "services.properties";
        int interval = intFromEnv("CHECK_INTERVAL_SECONDS", 60);
        int threshold = intFromEnv("FAILURE_THRESHOLD", 3);

        List<Service> services = ConfigLoader.loadServices(configPath);
        if (services.isEmpty()) {
            System.err.println("No services found in " + configPath);
            return;
        }

        String table = System.getenv("DYNAMODB_TABLE");
        String topicArn = System.getenv("SNS_TOPIC_ARN");

        ResultStore store = (table == null || table.isBlank())
                ? new ConsoleResultStore()
                : new DynamoDbResultStore(DynamoDbClient.create(), table);
        Alerter alerter = (topicArn == null || topicArn.isBlank())
                ? new ConsoleAlerter()
                : new SnsAlerter(SnsClient.create(), topicArn);

        System.out.println("Monitoring " + services.size() + " services every " + interval
                + "s (DOWN after " + threshold + " failures in a row)");
        System.out.println("Storage: " + store.getClass().getSimpleName()
                + " | Alerts: " + alerter.getClass().getSimpleName());

        Monitor monitor = new Monitor(services, new HealthChecker(Duration.ofSeconds(5)),
                new StatusTracker(threshold), store, alerter, interval);
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
        monitor.start();
    }

    private static int intFromEnv(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Ignoring invalid " + name + "=" + value);
            return fallback;
        }
    }
}

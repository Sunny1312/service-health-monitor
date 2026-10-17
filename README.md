# Service Health Monitor

A Java service that watches websites and APIs, saves every check to **Amazon DynamoDB**,
and emails an alert through **Amazon SNS** when a service goes down or recovers.

Built to practice outage detection: spot problems early, avoid false alarms, and keep a history
you can use to find the root cause.

## How it works

```
                 every N seconds
 services.properties ──► Monitor ──► HealthChecker ──► HTTP GET each service
                            │                                │
                            │                         CheckResult (status, latency, error)
                            │                                │
                            ├──► ResultStore ──► DynamoDB (history, auto-deleted after 7 days)
                            │
                            └──► StatusTracker ──► DOWN after N failures in a row?
                                                        │ yes (state changed)
                                                        ▼
                                                  Alerter ──► SNS ──► your email
```

| Class | Job |
|---|---|
| `HealthChecker` | Sends one HTTP request, measures latency, never throws |
| `StatusTracker` | Declares DOWN only after N failures in a row; alerts once per change, not every minute |
| `Monitor` | Schedules checks, saves results, triggers alerts |
| `DynamoDbResultStore` / `SnsAlerter` | AWS versions of storage and alerting |
| `ConsoleResultStore` / `ConsoleAlerter` | Local versions, used when AWS is not configured |

**Design choices**
- **Consecutive-failure threshold** (default 3) so one slow request doesn't cause a false alarm.
- **Alert on state change only**, so an outage sends one DOWN email and one RECOVERED email.
- **Storage or alert failures never stop monitoring**; they are logged and the loop continues.
- **Interfaces for storage and alerts**, so the AWS pieces can be swapped or tested without AWS.

## Run it locally (no AWS needed)

Requirements: Java 17+ and Maven.

```bash
mvn package
java -jar target/health-monitor.jar services.properties
```

Edit `services.properties` to list your own services (`Name=URL`). The `BrokenDemo` entry always
fails, so you will see a DOWN alert in the console after 3 checks.

Run the unit tests with `mvn test`.

## Configuration (environment variables)

| Variable | Default | Meaning |
|---|---|---|
| `CHECK_INTERVAL_SECONDS` | 60 | How often each service is checked |
| `FAILURE_THRESHOLD` | 3 | Failures in a row before a service is DOWN |
| `DYNAMODB_TABLE` | (none) | Save results to this table; if unset, prints to console |
| `SNS_TOPIC_ARN` | (none) | Send alerts to this topic; if unset, prints to console |

## Run it on AWS

1. **DynamoDB:** create a table, e.g. `HealthChecks`, partition key `serviceName` (String), sort key
   `checkedAt` (String), on-demand capacity. Optionally enable **TTL** on the attribute `expiresAt`.
2. **SNS:** create a Standard topic `health-alerts`, add an **Email** subscription, and confirm it
   from the email AWS sends you. Copy the topic ARN.
3. **IAM:** give the machine a role (or user) allowed to run `dynamodb:PutItem` on the table and
   `sns:Publish` on the topic. Use least privilege, not admin.
4. **EC2:** launch a small Linux instance with that role attached, install Java 17, copy
   `health-monitor.jar` and `services.properties` to it.
5. Run it:
   ```bash
   export AWS_REGION=ap-south-1
   export DYNAMODB_TABLE=HealthChecks
   export SNS_TOPIC_ARN=arn:aws:sns:ap-south-1:<account-id>:health-alerts
   nohup java -jar health-monitor.jar services.properties > monitor.log 2>&1 &
   ```
6. Check the DynamoDB table in the console to see results arriving, and temporarily add a bad URL to
   test the email alert.

**Cost:** DynamoDB on-demand, SNS email, and a small instance are very cheap at this scale. Use the
AWS Free plan, set billing alerts, and stop or terminate the instance when you are done.

## Ideas to extend

- Query a service's last 24 hours of latency and print p50/p95 latency.
- Add a small REST endpoint (`/status`) that shows the current state of each service.
- Check response body content, not just the status code.
- Package as a Docker image or run the checks from Lambda on an EventBridge schedule.

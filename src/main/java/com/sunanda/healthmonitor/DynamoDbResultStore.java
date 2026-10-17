package com.sunanda.healthmonitor;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Saves each check result as one item in a DynamoDB table.
 *
 * Table design:  partition key = serviceName (String)
 *                sort key      = checkedAt   (String, ISO-8601 time)
 * So "all checks for service X, newest first" is one cheap query.
 */
public class DynamoDbResultStore implements ResultStore {

    private static final long KEEP_DAYS = 7;

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoDbResultStore(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }

    @Override
    public void save(CheckResult r) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("serviceName", str(r.serviceName()));
        item.put("checkedAt", str(r.checkedAt().toString()));
        item.put("url", str(r.url()));
        item.put("up", AttributeValue.builder().bool(r.up()).build());
        item.put("statusCode", num(r.statusCode()));
        item.put("latencyMs", num(r.latencyMs()));
        if (r.error() != null) {
            item.put("error", str(r.error()));
        }
        // Optional DynamoDB TTL: old rows delete themselves so storage stays tiny.
        item.put("expiresAt", num(r.checkedAt().plusSeconds(KEEP_DAYS * 86_400).getEpochSecond()));

        client.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    private static AttributeValue str(String v) {
        return AttributeValue.builder().s(v).build();
    }

    private static AttributeValue num(long v) {
        return AttributeValue.builder().n(Long.toString(v)).build();
    }
}

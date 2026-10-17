package com.sunanda.healthmonitor;

/** Somewhere to save check results. Implemented for the console and for DynamoDB. */
public interface ResultStore {
    void save(CheckResult result);
}

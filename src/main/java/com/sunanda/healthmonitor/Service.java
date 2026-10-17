package com.sunanda.healthmonitor;

/** A service we want to watch: a friendly name and the URL to check. */
public record Service(String name, String url) {}

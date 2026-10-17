package com.sunanda.healthmonitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Reads the list of services to monitor from a simple Name=URL file. */
public class ConfigLoader {

    public static List<Service> loadServices(String path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            props.load(in);
        }
        List<Service> services = new ArrayList<>();
        for (String name : props.stringPropertyNames()) {
            services.add(new Service(name.trim(), props.getProperty(name).trim()));
        }
        services.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return services;
    }
}

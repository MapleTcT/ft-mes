package com.mapletct.ftmes.womprint.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PrinterConfig {

    private final long id;
    private final String printName;
    private final String host;
    private final Integer port;

    public PrinterConfig(long id, String printName, String host, Integer port) {
        this.id = id;
        this.printName = printName;
        this.host = host;
        this.port = port;
    }

    public long getId() {
        return id;
    }

    public String getPrintName() {
        return printName;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", id);
        result.put("printName", printName);
        result.put("aimHost", host);
        result.put("port", port);
        return result;
    }
}

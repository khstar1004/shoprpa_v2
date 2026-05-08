package com.iflytek.rpa.auth.health;

import java.net.HttpURLConnection;
import java.net.URL;

public final class HealthcheckProbe {

    private static final String DEFAULT_URL = "http://127.0.0.1:10251/api/rpa-auth/health";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private HealthcheckProbe() {}

    public static void main(String[] args) throws Exception {
        String healthUrl = args.length > 0 ? args[0] : DEFAULT_URL;
        int timeoutMs = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TIMEOUT_MS;

        HttpURLConnection connection = (HttpURLConnection) new URL(healthUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Healthcheck failed with HTTP " + status + " for " + healthUrl);
        }
    }
}

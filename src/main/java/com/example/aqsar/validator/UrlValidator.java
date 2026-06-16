package com.example.aqsar.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class UrlValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlValidator.class);

    public boolean isValid(String url) {
        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();
            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.warn("Unsupported scheme: {}", url);
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.equalsIgnoreCase("localhost")) {
                log.warn("Invalid host: {}", url);
                return false;
            }

            InetAddress address = InetAddress.getByName(host);

            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()) {
                log.warn("Blocked private IP URL: {}", url);
                return false;
            }

//            HttpURLConnection connection =
//                    (HttpURLConnection) uri.toURL().openConnection();
//
//            connection.setRequestMethod("HEAD");
//            connection.setConnectTimeout(5000);
//            connection.setReadTimeout(5000);
//            connection.setInstanceFollowRedirects(true);
//
//            int statusCode = connection.getResponseCode();
//            System.out.println("status  "+statusCode);
//
            return true;

        } catch (Exception e) {
            log.warn("Validation failed for {}", url, e);
            return false;
        }
    }
}
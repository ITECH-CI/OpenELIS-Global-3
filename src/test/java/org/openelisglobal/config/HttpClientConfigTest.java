package org.openelisglobal.config;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Unit tests for the simplified-TLS behavior of {@link HttpClientConfig}: the
 * HTTP client must build even when no client keystore/truststore is configured
 * (internal traffic is plain HTTP; outgoing HTTPS uses the JVM default trust
 * store). Guards against a Spring-context startup failure.
 */
public class HttpClientConfigTest {

    private void setField(HttpClientConfig config, String name, Object value) throws Exception {
        Field f = HttpClientConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(config, value);
    }

    @Test
    public void httpClientBuilds_whenNoKeystoreConfigured() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        // Simulate absent SSL material (empty @Value defaults).
        setField(config, "trustStore", null);
        setField(config, "keyStore", null);
        setField(config, "trustStorePassword", "");
        setField(config, "keyStorePassword", "");
        setField(config, "keyPassword", "");

        CloseableHttpClient client = config.httpClient();
        assertNotNull("HTTP client should build without any keystore", client);
    }

    @Test
    public void httpClientBuilds_whenKeystorePointsToMissingFile() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        // A configured-but-nonexistent path must be treated as "no material",
        // not crash (isUsable() returns false).
        Resource missing = new FileSystemResource("/does/not/exist.keystore");
        setField(config, "trustStore", missing);
        setField(config, "keyStore", missing);
        setField(config, "trustStorePassword", "x");
        setField(config, "keyStorePassword", "x");
        setField(config, "keyPassword", "x");

        CloseableHttpClient client = config.httpClient();
        assertNotNull("HTTP client should build when keystore file is missing", client);
    }
}

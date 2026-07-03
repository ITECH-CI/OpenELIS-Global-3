package org.openelisglobal.config;

import javax.net.ssl.SSLContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class HttpClientConfig {

    // Client-certificate (mTLS) material is OPTIONAL: with the simplified TLS
    // architecture, inter-container traffic (FHIR, etc.) is plain HTTP on a
    // private Docker network, so no client keystore/truststore is required.
    // Defaults are empty so the bean still builds when these properties are
    // absent; outgoing HTTPS then relies on the JVM default trust store.
    @Value("${server.ssl.trust-store:}")
    private Resource trustStore;

    @Value("${server.ssl.trust-store-password:}")
    private String trustStorePassword;

    @Value("${server.ssl.key-store:}")
    private Resource keyStore;

    @Value("${server.ssl.key-store-password:}")
    private String keyStorePassword;

    @Value("${server.ssl.key-password:}")
    private String keyPassword;

    @Value("${org.openelisglobal.httpclient.connectionRequestTimeout:0}")
    private Integer connectionRequestTimeout;

    @Value("${org.openelisglobal.httpclient.connectionTimeout:0}")
    private Integer connectionTimeout;

    @Value("${org.openelisglobal.httpclient.socketTimeout:0}")
    private Integer socketTimeout;

    @Bean
    public CloseableHttpClient httpClient() throws Exception {

        HttpClientBuilder httpBuilder = HttpClientBuilder.create().setSSLSocketFactory(sslConnectionSocketFactory());

        if (connectionRequestTimeout != 0 || connectionTimeout != 0 || socketTimeout != 0) {
            final RequestConfig.Builder configBuilder = RequestConfig.custom();
            if (connectionRequestTimeout != 0) {
                configBuilder.setConnectionRequestTimeout(this.connectionRequestTimeout);
            }
            if (connectionTimeout != 0) {
                configBuilder.setConnectTimeout(connectionTimeout);
            }
            if (socketTimeout != 0) {
                configBuilder.setSocketTimeout(socketTimeout);
            }
            httpBuilder.setDefaultRequestConfig(configBuilder.build());
        }

        return httpBuilder.setSSLSocketFactory(sslConnectionSocketFactory()).build();
    }

    public SSLConnectionSocketFactory sslConnectionSocketFactory() throws Exception {
        return new SSLConnectionSocketFactory(sslContext());
    }

    public SSLContext sslContext() throws Exception {
        SSLContextBuilder builder = SSLContextBuilder.create();

        // Load client key material only when a real, readable keystore is
        // configured. When absent (simplified TLS, no internal keystores), skip
        // it so the context still builds and outgoing HTTPS uses the JVM default
        // trust store.
        if (isUsable(keyStore)) {
            builder.loadKeyMaterial(keyStore.getFile(), toChars(keyStorePassword), toChars(keyPassword));
        } else {
            LogEvent.logInfo("HttpClientConfig", "sslContext",
                    "No client keystore configured — building HTTP client without mTLS key material");
        }

        if (isUsable(trustStore)) {
            builder.loadTrustMaterial(trustStore.getFile(), toChars(trustStorePassword));
        }

        return builder.build();
    }

    private boolean isUsable(Resource resource) {
        try {
            return resource != null && resource.exists() && resource.getFile().length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private char[] toChars(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }
}

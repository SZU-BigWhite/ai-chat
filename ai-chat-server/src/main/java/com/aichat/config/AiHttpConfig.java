package com.aichat.config;

import java.net.http.HttpClient;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/** 模型 HTTP 连接和读取超时配置。 */
@Configuration
public class AiHttpConfig {
    @Bean
    RestClientCustomizer aiTimeouts(AiLimitProperties properties) {
        return builder -> {
            var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build());
            factory.setReadTimeout(properties.getReadTimeout());
            builder.requestFactory(factory);
        };
    }
}

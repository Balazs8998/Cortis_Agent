package dev.rehm.lingua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMinutes(3));

        return RestClient.builder()
                .baseUrl("http://localhost:11434")
                .requestFactory(factory)
                .build();
    }
}

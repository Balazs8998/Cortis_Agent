package dev.rehm.lingua.searxng;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class SearxngConfig {

    @Bean
    public RestClient SearxngRestClient() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofMinutes(3));

        return RestClient.builder()
                .baseUrl("http://localhost:8888")
                .requestFactory(factory)
                .build();
    }
}

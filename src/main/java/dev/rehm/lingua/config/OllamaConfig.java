package dev.rehm.lingua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient(RestClient.Builder builder){
        String url = "http://localhost:11434";


        builder.baseUrl(url);
        return builder.build();

    }
}

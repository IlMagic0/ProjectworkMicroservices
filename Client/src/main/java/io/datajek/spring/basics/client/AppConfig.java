package io.datajek.spring.basics.client;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    @LoadBalanced  // Enables Eureka service name resolution in RestTemplate calls
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

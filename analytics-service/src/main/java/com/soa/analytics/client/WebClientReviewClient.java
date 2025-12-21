package com.soa.analytics.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class WebClientReviewClient implements ReviewClient {
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public WebClientReviewClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public List<ReviewClientDto> findByTourId(Long tourId) {
        // Call review-service via service discovery through gateway or directly with lb://review-service
        String url = "http://review-service/api/reviews/tour/" + tourId;
        // Note: Using plain HTTP with service name relies on container DNS; inside Docker compose, http://review-service should work.
        Mono<ReviewClientDto[]> mono = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(ReviewClientDto[].class);
        ReviewClientDto[] arr = mono.block();
        return arr == null ? List.of() : Arrays.asList(arr);
    }
}

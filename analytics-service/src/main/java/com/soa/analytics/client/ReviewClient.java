package com.soa.analytics.client;

import java.util.List;

public interface ReviewClient {
    List<ReviewClientDto> findByTourId(Long tourId);
}

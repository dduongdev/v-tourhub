package com.v_tourhub.booking_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Cố gắng lấy khóa (Lock)
     * @param key: Tên khóa (ví dụ: lock:inventory:service:101)
     * @param ttlSeconds: Thời gian tự động nhả khóa (tránh deadlock nếu server sập)
     * @return true nếu lấy được khóa, false nếu người khác đang giữ
     */
    public boolean tryLock(String key, long ttlSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}
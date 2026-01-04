package com.v_tourhub.booking_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua script để unlock an toàn.
     * Chỉ xóa key nếu value khớp với token được cung cấp.
     * Điều này ngăn chặn việc unlock nhầm lock của request khác.
     */
    private static final String UNLOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";

    /**
     * Cố gắng lấy khóa với unique token.
     * 
     * @param key        Tên khóa (ví dụ: lock:inventory:101:2024-01-15)
     * @param lockToken  Unique token để identify owner của lock
     * @param ttlSeconds Thời gian tự động nhả khóa (tránh deadlock nếu server
     *                   crash)
     * @return true nếu lấy được khóa, false nếu người khác đang giữ
     */
    public boolean tryLock(String key, String lockToken, long ttlSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, lockToken, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Nhả khóa một cách an toàn với token validation.
     * Sử dụng Lua script để đảm bảo atomic check-and-delete.
     * Chỉ xóa lock nếu token khớp với giá trị đang lưu trong Redis.
     * 
     * @param key           Tên khóa cần unlock
     * @param expectedToken Token mong đợi (phải khớp với token đã dùng khi lock)
     * @return true nếu unlock thành công, false nếu token không khớp hoặc key không
     *         tồn tại
     */
    public boolean unlock(String key, String expectedToken) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(key), expectedToken);

        if (result != null && result == 1) {
            return true;
        } else {
            log.warn("Failed to unlock key={}. Token mismatch or key expired.", key);
            return false;
        }
    }

    // ========== DEPRECATED METHODS (backward compatibility) ==========

    /**
     * @deprecated Use {@link #tryLock(String, String, long)} với unique token thay
     *             thế.
     *             Method này không an toàn trong môi trường distributed.
     */
    @Deprecated
    public boolean tryLockLegacy(String key, long ttlSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    /**
     * @deprecated Use {@link #unlock(String, String)} với token validation thay
     *             thế.
     *             Method này có thể unlock nhầm lock của request khác.
     */
    @Deprecated
    public void unlockLegacy(String key) {
        redisTemplate.delete(key);
    }
}
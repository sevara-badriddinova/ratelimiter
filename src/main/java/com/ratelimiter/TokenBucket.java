package com.ratelimiter;


import org.springframework.data.redis.core.RedisTemplate;

public class TokenBucket {
    int maxCapacity;
    int refillRate;
    long tokensAvailable;
    long lastRefill;
    String userId;
    final RedisTemplate<String, Long> redisTemplate;

    public TokenBucket(int maxCapacity, int refillRate, String userId, RedisTemplate<String, Long> redisTemplate) {
        this.maxCapacity = maxCapacity;
        this.refillRate = refillRate;
        this.tokensAvailable = maxCapacity;
        this.lastRefill = System.currentTimeMillis();
        this.redisTemplate = redisTemplate;
        this.userId = userId;
    }

    public boolean allowRequest() {
        refill();
        if (tokensAvailable >= 1) {
            tokensAvailable -= 1;
            redisTemplate.opsForValue().set("rate:" + userId + ":tokens", tokensAvailable);
            return true;
        } else {
            return false;
        }
    }

    public void refill() {
        Long lastRefill = redisTemplate.opsForValue().get("rate:" + userId + ":lastRefill");
        if(lastRefill == null){
            lastRefill = System.currentTimeMillis();
        }
        Long tokens = redisTemplate.opsForValue().get("rate:" + userId + ":tokens");
        if (tokens == null){
            tokens = (long)maxCapacity;
        }
        tokensAvailable = tokens;
        if (tokensAvailable <= 0){
            tokensAvailable = 0;
        }
        if (tokensAvailable < maxCapacity) {
            long tokensToAdd = refillRate * (System.currentTimeMillis() - lastRefill) / 1000;
            tokensAvailable = Math.min(maxCapacity, tokensToAdd + tokensAvailable);
        }
        redisTemplate.opsForValue().set("rate:" + userId + ":tokens", tokensAvailable);
        redisTemplate.opsForValue().set("rate:" + userId + ":lastRefill", System.currentTimeMillis());
    }
}


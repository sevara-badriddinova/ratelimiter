package com.ratelimiter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    @Autowired
    RedisTemplate<String, Long> redisTemplate;
    @Autowired
    MeterRegistry meterRegistry;
    ConcurrentHashMap<String, TokenBucket> tokenBuckets;
    Counter tokensAllowed;
    Counter tokensRejected;

    @PostConstruct
    public void init(){
        tokensAllowed = meterRegistry.counter("rate_limiter_allowed");
        tokensRejected = meterRegistry.counter("rate_limiter_rejected");
    }

    RateLimiterService() {
        this.tokenBuckets = new ConcurrentHashMap<>();
    }
    public boolean allowRequest(String userId){
        TokenBucket bucket;
        if (tokenBuckets.containsKey(userId)){
            bucket = tokenBuckets.get(userId);
        }
        else{
            bucket = new TokenBucket(5, 2, userId, redisTemplate);
            tokenBuckets.put(userId, bucket);
        }
        boolean result = bucket.allowRequestAtomic();
        if (result){
            tokensAllowed.increment();
        }
        else{
            tokensRejected.increment();
        }
        return result;
    }
}

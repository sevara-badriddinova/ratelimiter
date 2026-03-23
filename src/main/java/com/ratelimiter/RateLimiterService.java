package com.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    @Autowired
    RedisTemplate<String, Long> redisTemplate;
    ConcurrentHashMap<String, TokenBucket> tokenBuckets;

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
        return bucket.allowRequestAtomic();
    }
}

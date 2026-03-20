package com.ratelimiter;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
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
            bucket = new TokenBucket(5, 2);
            tokenBuckets.put(userId, bucket);
        }
        return bucket.allowRequest();
    }
}

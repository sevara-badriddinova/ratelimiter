package com.ratelimiter;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

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

    // Lua script
    public boolean allowRequestAtomic(){
        // ARGV[1] - current time, ARGV[2] - refillRate, tonumber() - converts string values from Redis to numbers
        String luaScript =
                "local tokensAvailable = redis.call('GET', KEYS[1]) " +
                "local lastRefill = redis.call('GET', KEYS[2]) " +
                "if tokensAvailable == false then tokensAvailable = ARGV[3] end " +
                "if lastRefill == false then lastRefill = ARGV[1] end " +
                "local tokensToAdd = tonumber(ARGV[2]) * (tonumber(ARGV[1]) - tonumber(lastRefill)) / 1000 " +
                "tokensAvailable = math.min(tonumber(ARGV[3]), tokensToAdd + tonumber(tokensAvailable)) " +
                "if tokensAvailable >= 1 then " +
                "tokensAvailable = tokensAvailable - 1 " +
                "redis.call('SET', KEYS[1], tokensAvailable) " +
                "redis.call('SET', KEYS[2], ARGV[1]) " +
                "return 1 " +
                "else return 0 end";

        // passing redis keys lua script needs
        List<String> keys = List.of("rate:" + userId + ":tokens", "rate:" + userId + ":lastRefill");
        // argv values Lua needs, redis stores strings, so convert w String.valueof()
        String[] args = {String.valueOf(System.currentTimeMillis()), String.valueOf(refillRate), String.valueOf(maxCapacity)};
        // wraps Lua string into script Spring can execute
        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);
        // sends scripts + keys + args to redis
        Long result = redisTemplate.execute(script, keys, args);
        return result == 1L;
    }
}


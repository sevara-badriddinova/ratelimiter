package com.ratelimiter;


public class TokenBucket {
    int maxCapacity;
    int refillRate;
    long tokensAvailable;
    long lastRefill = 0;

    public TokenBucket(int maxCapacity, int refillRate) {
        this.maxCapacity = maxCapacity;
        this.refillRate = refillRate;
        this.tokensAvailable = maxCapacity;
        this.lastRefill = System.currentTimeMillis();
    }

    public boolean allowRequest() {
        refill();
        if (tokensAvailable >= 1) {
            tokensAvailable -= 1;
            return true;
        } else {
            return false;
        }
    }

    public void refill() {
        if (tokensAvailable < maxCapacity) {
            long tokensToAdd = refillRate * (System.currentTimeMillis() - lastRefill) / 1000;
            tokensAvailable = Math.min(maxCapacity, tokensToAdd + tokensAvailable);
        }
        lastRefill = System.currentTimeMillis();
    }
}


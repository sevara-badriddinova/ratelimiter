package com.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimiterController {
    @Autowired
    RateLimiterService rateLimiterService;

    @GetMapping("/api/request")
    public ResponseEntity<String> processUser(@RequestHeader("X-User-Id") String userId){
        if (rateLimiterService.allowRequest(userId)) {
            return ResponseEntity.status(HttpStatus.OK).body("allow request successful");
        }else{
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("request limit exceeded");
        }
    }

}

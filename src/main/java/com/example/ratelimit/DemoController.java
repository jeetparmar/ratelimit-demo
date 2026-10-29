package com.example.ratelimit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of("message", "hello", "time", Instant.now().toString());
    }
}

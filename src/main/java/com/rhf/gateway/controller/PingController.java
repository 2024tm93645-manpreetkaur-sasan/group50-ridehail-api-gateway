package com.rhf.gateway.controller;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Mono<Map<String, String>> ping() {
        LoggerFactory.getLogger(PingController.class).info("PingController Executed Successfully!");
        return Mono.just(Map.of("status", "UP"));
    }
}

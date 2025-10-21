package com.exp2.api.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.UnifiedJedis;

@Component
public class JedisConnectionManager {

    private UnifiedJedis jedis;

    @Value("${spring.data.redis.host}")
    String redisHost;

    @Value("${spring.data.redis.port}")
    int redisPort;

    public JedisConnectionManager() {

    }

    @PostConstruct
    public void initialize() {
        String redisUrl = String.format("redis://%s:%d",redisHost,redisPort);
        this.jedis = new UnifiedJedis(redisUrl);
    }

    public UnifiedJedis getJedis() {
        return this.jedis;
    }

    @PreDestroy
    public void closeConnection() {
        if (this.jedis != null) {
            System.out.println("Closing connection");
            this.jedis.close();
        }
    }
}
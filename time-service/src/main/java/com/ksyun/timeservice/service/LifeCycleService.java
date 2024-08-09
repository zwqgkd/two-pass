package com.ksyun.timeservice.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org. springframework.http.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Getter
public class LifeCycleService {

    private final RestTemplate restTemplate;

    @Value("${my-register.application.name}")
    private String serviceName;

    private final String serviceId;

    @Value("${server.port}")
    private int servicePort;

    @Value("${my-register.discovery.server-addr}")
    private String discoveryServerAddr;

    // 获取本机IP地址
    private final String serviceHost;

    @Autowired
    public LifeCycleService(RestTemplate restTemplate) throws UnknownHostException {
        this.serviceId=UUID.randomUUID().toString();
        this.serviceHost = InetAddress.getLocalHost().getHostAddress();
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void register(){
        log.info("register service info: serviceName:{}, serviceId:{}," +
                "host:{}, port:{}", serviceName, serviceId, serviceHost, servicePort);
        postToDiscoveryServer("/api/register");
    }

    @PreDestroy
    public void unregister(){
        log.info("unregister service info: serviceName:{}, serviceId:{}," +
                "host:{}, port:{}", serviceName, serviceId, serviceHost, servicePort);
        postToDiscoveryServer("/api/unregister");
    }

    @Scheduled(fixedRate = 30000)
    public void heartbeat(){
        log.info("heartbeat service info: serviceName:{}, serviceId:{}," +
                "host:{}, port:{}", serviceName, serviceId, serviceHost, servicePort);
        postToDiscoveryServer("/api/heartbeat");
    }

    public void postToDiscoveryServer(String ctl){
        HttpHeaders headers = new HttpHeaders();

        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("serviceName",serviceName);
        requestBody.put("serviceId",serviceId);
        requestBody.put("ipAddress",serviceHost);
        requestBody.put("port",servicePort);

        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(requestBody,headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                "http://"+discoveryServerAddr+ctl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }
}

package com.ksyun.registry.controller;

import com.ksyun.registry.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.ksyun.registry.model.ServiceInfo;

@Slf4j
@RestController
@RequestMapping("/api")
public class RegistryController {
    private final Map<String, List<ServiceInfo>> serviceNameRegistry = new ConcurrentHashMap<>();
    // 轮询计数器，用于记录每个服务名的轮询位置
    private final Map<String, AtomicInteger> pollingIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> heartbeatTimestamps = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_TIMEOUT_MS = 60000; // 60 seconds

    @PostMapping("/register")
    public Response register(@RequestBody ServiceInfo serviceInfo) {
        serviceNameRegistry.computeIfAbsent(serviceInfo.getServiceName(), k -> new CopyOnWriteArrayList<>())
                .add(serviceInfo);
//        heartbeatTimestamps.put(serviceInfo.getServiceId(), System.currentTimeMillis());

        log.info("Service registered: {}", serviceInfo.toString());
        return new Response(Response.StatusCode.SUCCESS, "Service registered");
    }

    @PostMapping("/unregister")
    public Response unregister(@RequestBody ServiceInfo serviceInfo) {
        boolean unregistered = false;
        List<ServiceInfo> instances = serviceNameRegistry.get(serviceInfo.getServiceName());
        if (instances != null) {
            for (ServiceInfo instance : instances) {
                if (instance.getServiceId().equals(serviceInfo.getServiceId()) && instance.getIp().equals(serviceInfo.getIp()) && instance.getPort() == serviceInfo.getPort()) {
                    instances.remove(instance);
                    heartbeatTimestamps.remove(serviceInfo.getServiceId());
                    unregistered = true;
                    break;
                }
            }
            if (instances.isEmpty()) {
                serviceNameRegistry.remove(serviceInfo.getServiceName());
            }
        }
        if(!unregistered){
            log.info("Service instance is not registered before request unregister: {}", serviceInfo.toString());
            return new Response(Response.StatusCode.FAILURE, "Service not registered");
        }else{
            log.info("Service unregistered: {}", serviceInfo.toString());
            return new Response(Response.StatusCode.SUCCESS, "Service unregistered");
        }
    }

    @PostMapping("/heartbeat")
    public Response heartbeat(@RequestBody ServiceInfo serviceInfo) {
        List<ServiceInfo> allInstances=new ArrayList<>();
        serviceNameRegistry.values().forEach(allInstances::addAll);
        if(allInstances.stream().noneMatch(item->item.getServiceId().equals(serviceInfo.getServiceId()) && item.getIp().equals(serviceInfo.getIp()) && item.getPort()==serviceInfo.getPort())){
            log.info("Service instance is not registered before request hearbeat: {}", serviceInfo.toString());
            return new Response(Response.StatusCode.FAILURE, "Service not registered");
        }
        //如果是第一次记录时间
        if(!heartbeatTimestamps.containsKey(serviceInfo.getServiceId())){
            heartbeatTimestamps.put(serviceInfo.getServiceId(), System.currentTimeMillis());
            log.info("First heartbeat received: {}", serviceInfo.toString());
            return new Response(Response.StatusCode.SUCCESS, "First heartbeat received");
        }else{
            long now = System.currentTimeMillis();
            if (now - heartbeatTimestamps.get(serviceInfo.getServiceId()) > HEARTBEAT_TIMEOUT_MS) {
                this.unregister(serviceInfo);
                log.info("Heartbeat timeout, then unregistered: {}", serviceInfo.toString());
                return new Response(Response.StatusCode.FAILURE, "Heartbeat timeout, then unregistered");
            }
            heartbeatTimestamps.put(serviceInfo.getServiceId(), System.currentTimeMillis());
            log.info("Heartbeat received: {}", serviceInfo.toString());
            return new Response(Response.StatusCode.SUCCESS, "Heartbeat received");
        }
    }

    @GetMapping("/discovery")
    public List<ServiceInfo> discovery(@RequestParam(required = false) String name) {
        if (name == null) {
            // 如果没有指定服务名，返回所有服务实例
            List<ServiceInfo> allInstances = new CopyOnWriteArrayList<>();
            serviceNameRegistry.values().forEach(allInstances::addAll);
            //unregister timeout instances
            allInstances.stream().filter(item -> !this.isInstanceAlive(item.getServiceId()))
                    .collect(Collectors.toList())
                    .forEach(this::unregister);
            return allInstances.stream()
                    .filter(item -> this.isInstanceAlive(item.getServiceId())) // 过滤掉不活跃的服务实例
                    .collect(Collectors.toList());
        } else {
            // 如果指定了服务名，使用轮询逻辑选择一个实例
            List<ServiceInfo> instances = serviceNameRegistry.getOrDefault(name, new CopyOnWriteArrayList<>());
            //unregister timeout instances
            instances.stream().filter(item -> !this.isInstanceAlive(item.getServiceId()))
                    .forEach(this::unregister);
            //filter
            List<ServiceInfo> aliveInstances = instances.stream()
                    .filter(item -> this.isInstanceAlive(item.getServiceId())) // 过滤掉不活跃的服务实例
                    .collect(Collectors.toList());
            //标准轮询
            AtomicInteger index = pollingIndex.computeIfAbsent(name, k -> new AtomicInteger(0));
            if (aliveInstances.isEmpty()) {
                return null; // 如果没有活跃的服务实例，返回null
            }
            ServiceInfo serviceInfo = aliveInstances.get(index.getAndIncrement() % aliveInstances.size());
            return Collections.singletonList(serviceInfo); // 返回一个包含单个服务实例的列表
        }
    }

    private boolean isInstanceAlive(String serviceId) {
        // 假设每个ServiceInfo对象都有一个lastHeartbeat字段，这里检查它是否在合理的时间范围内
        if(!heartbeatTimestamps.containsKey(serviceId)){
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return currentTime - heartbeatTimestamps.get(serviceId) < HEARTBEAT_TIMEOUT_MS;
    }
}

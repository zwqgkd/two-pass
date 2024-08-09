package com.ksyun.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceInfo {

    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("ipAddress")
    private String ip;

    @JsonProperty("port")
    int port;
}

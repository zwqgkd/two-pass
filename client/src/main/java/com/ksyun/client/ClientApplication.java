package com.ksyun.client;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import com.ksyun.client.model.ServiceInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@EnableScheduling
@Slf4j
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @RestController
    @RequestMapping("/api")
    public static class InfoController {

        private final RestTemplate restTemplate;

        private final String timeServiceName = "time-service";

        @Value("${my-register.discovery.server-addr}")
        private String discoveryServerAddr;

        @Autowired
        public InfoController(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        @GetMapping("/getInfo")
        public InfoResponse getInfo() {
            InfoResponse response = new InfoResponse();
            //get service info from registry server
            ResponseEntity<List<ServiceInfo>> responseEntity = restTemplate.exchange(
                    "http://" + discoveryServerAddr + "/api/discovery" + "?name=" + timeServiceName,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ServiceInfo>>() {
                    }
            );

            //request time service
            if (responseEntity.getBody() == null || responseEntity.getBody().isEmpty()) {
                log.info("No available service");
                response.setError("No available service");
            } else {
                ServiceInfo serviceInfo = responseEntity.getBody().get(0);
                log.info("Get service info: {}", serviceInfo.toString());
                String url = "http://" + serviceInfo.getIp() + ":" + serviceInfo.getPort() + "/api/getDateTime" + "?style=full";
                log.info("Request time service url: {}", url);
                ResponseEntity<DateTimeResponse> timeResponseEntity = restTemplate.exchange(
                        url,
                        org.springframework.http.HttpMethod.GET,
                        null,
                        DateTimeResponse.class
                );
                String gmtTimeStr =Objects.requireNonNull(timeResponseEntity.getBody()).getResult();
                //tranform time to beijing time
                // 创建一个DateTimeFormatter来解析GMT时间字符串
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("GMT"));

                // 解析字符串以创建ZonedDateTime对象
                ZonedDateTime gmtZonedDateTime = ZonedDateTime.parse(gmtTimeStr, formatter);

                // 将GMT时间转换为北京时间
                ZonedDateTime beijingTime = gmtZonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));

                String beijingTimeStr = beijingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                response.setResult("Hello Kingsoft Cloud Star Camp - [" + responseEntity.getBody().get(0).getServiceId() + "] - " + beijingTimeStr);
            }
            return response;
        }

        @Setter
        @Getter
        @ToString
        @NoArgsConstructor
        @AllArgsConstructor
        static class InfoResponse {
            private String error;
            private String result;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        static class DateTimeResponse {
            private String result;
            private String serviceId;
        }

    }
}

package com.ksyun.timeservice.controller;


import com.ksyun.timeservice.service.LifeCycleService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@RestController
@RequestMapping("/api")
public class TimeController {

    private final LifeCycleService lifeCycleService;

    @Autowired
    public TimeController(LifeCycleService lifeCycleService) {
        this.lifeCycleService=lifeCycleService;
    }



    @GetMapping("/getDateTime")
    public DateTimeResponse getDateTime(@RequestParam(value = "style") String style) {
        LocalDateTime now = LocalDateTime.now();
        //format

        DateTimeResponse response = new DateTimeResponse();
        response.setResult(formatDateTime(now, style));
        response.setServiceId(lifeCycleService.getServiceId());
        return response;
    }

    private String formatDateTime(LocalDateTime dateTime, String style) {
        DateTimeFormatter formatter;
        switch (style) {
            case "full":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return dateTime.format(formatter);
            case "date":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return dateTime.format(formatter);
            case "time":
                formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                return dateTime.format(formatter);
            case "unix":
                // 获取当前时间的 Unix 时间戳（毫秒）
                long unixTimeMillis = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                return String.valueOf(unixTimeMillis);
            default:
                // 默认返回 "full" 格式
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return dateTime.format(formatter);
        }
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class DateTimeResponse {
        private String result;
        private String serviceId;
    }
}

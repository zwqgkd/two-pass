package com.ksyun.registry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;


public class Response {
    @JsonProperty("code")
    private StatusCode statusCode;

    @JsonProperty("message")
    private String message;

    public Response(StatusCode statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    @Getter
    public enum StatusCode {
        SUCCESS(200, "请求成功"),
        FAILURE(500, "请求失败");
        private final int code;
        private final String description;

        StatusCode(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }
}


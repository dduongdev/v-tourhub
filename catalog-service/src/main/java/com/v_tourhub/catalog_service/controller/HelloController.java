package com.v_tourhub.catalog_service.controller;

import com.soa.common.dto.ApiResponse; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class HelloController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/hello")
    public ApiResponse<String> sayHello() {
        return ApiResponse.success("Xin chào! Đây là Catalog Service chạy trên port: " + port);
    }

    @GetMapping("/secure")
    public ApiResponse<String> saySecureHello() {
        return ApiResponse.success("CHÚC MỪNG! Bạn đã truy cập được API bảo mật với Token hợp lệ.");
    }
}
package com.v_tourhub.catalog_service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PublicTourismServiceDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String serviceType;
    private Boolean availability;

    // Thông tin về địa điểm cha
    private DestinationInfo destination;

    // Danh sách ảnh (chỉ trả về URL)
    private List<String> mediaUrls;

    // Danh sách thuộc tính (Key-Value)
    private Map<String, String> attributes;

     private List<InventoryInfo> inventoryCalendar;

    // DTO lồng nhau để chứa thông tin Destination
    @Data
    @Builder
    public static class DestinationInfo {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String province;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    public static class InventoryInfo {
        private LocalDate date;
        private int availableStock; // Số lượng khả dụng
        private int totalStock;
        private boolean isAvailable; // Cờ tiện ích (availableStock > 0)
    }
}
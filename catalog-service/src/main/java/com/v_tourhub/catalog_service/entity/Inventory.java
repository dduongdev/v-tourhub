package com.v_tourhub.catalog_service.entity;

import java.time.LocalDate;

import com.soa.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventories", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "service_id", "date" })
})
@Getter
@Setter
public class Inventory extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private TourismService service;

    @Column(nullable = false)
    private LocalDate date;

    private int totalStock;
    private int bookedStock;
    private Integer lockedStock;

    @Version
    private Long version; // Optimistic locking to prevent concurrent update issues

    public Integer getAvailableStock() {
        return totalStock - bookedStock - lockedStock;
    }
}
package com.example.rental.billing.entity;

import com.example.rental.common.audit.BaseEntity;
import com.example.rental.property.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "utility_readings")
public class UtilityReading extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "electricity_old_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal electricityOldReading = BigDecimal.ZERO;

    @Column(name = "electricity_new_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal electricityNewReading = BigDecimal.ZERO;

    @Column(name = "electricity_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityUnitPrice = BigDecimal.ZERO;

    @Column(name = "water_old_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal waterOldReading = BigDecimal.ZERO;

    @Column(name = "water_new_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal waterNewReading = BigDecimal.ZERO;

    @Column(name = "water_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUnitPrice = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Integer getBillingYear() {
        return billingYear;
    }

    public void setBillingYear(Integer billingYear) {
        this.billingYear = billingYear;
    }

    public Integer getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(Integer billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BigDecimal getElectricityOldReading() {
        return electricityOldReading;
    }

    public void setElectricityOldReading(BigDecimal electricityOldReading) {
        this.electricityOldReading = electricityOldReading;
    }

    public BigDecimal getElectricityNewReading() {
        return electricityNewReading;
    }

    public void setElectricityNewReading(BigDecimal electricityNewReading) {
        this.electricityNewReading = electricityNewReading;
    }

    public BigDecimal getElectricityUnitPrice() {
        return electricityUnitPrice;
    }

    public void setElectricityUnitPrice(BigDecimal electricityUnitPrice) {
        this.electricityUnitPrice = electricityUnitPrice;
    }

    public BigDecimal getWaterOldReading() {
        return waterOldReading;
    }

    public void setWaterOldReading(BigDecimal waterOldReading) {
        this.waterOldReading = waterOldReading;
    }

    public BigDecimal getWaterNewReading() {
        return waterNewReading;
    }

    public void setWaterNewReading(BigDecimal waterNewReading) {
        this.waterNewReading = waterNewReading;
    }

    public BigDecimal getWaterUnitPrice() {
        return waterUnitPrice;
    }

    public void setWaterUnitPrice(BigDecimal waterUnitPrice) {
        this.waterUnitPrice = waterUnitPrice;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public BigDecimal getElectricityUsage() {
        return electricityNewReading.subtract(electricityOldReading);
    }

    public BigDecimal getWaterUsage() {
        return waterNewReading.subtract(waterOldReading);
    }
}

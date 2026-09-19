package com.lab.center.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** 领用 / 退回登记。 */
@Entity
@Table(name = "usage_log")
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "log_no", nullable = false, length = 24, unique = true)
    public String logNo;

    @Column(name = "reagent_id", nullable = false)
    public Long reagentId;

    @Column(name = "user_name", nullable = false, length = 32)
    public String userName;

    @Column(name = "use_date")
    public LocalDate useDate;

    @Column(name = "quantity", nullable = false)
    public Integer quantity;

    /** 领用 / 退回 */
    @Column(name = "direction", nullable = false, length = 8)
    public String direction;

    @Column(name = "purpose", length = 64)
    public String purpose;
}

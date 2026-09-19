package com.lab.center.entity;

import com.lab.center.dto.BizException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 仪器设备。 */
@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "instrument_code", nullable = false, length = 24, unique = true)
    public String instrumentCode;

    @Column(name = "instrument_name", nullable = false, length = 64)
    public String instrumentName;

    @Column(name = "model_text", length = 32)
    public String modelText;

    @Column(name = "keeper", length = 32)
    public String keeper;

    @Column(name = "instrument_status", nullable = false, length = 16)
    public String instrumentStatus;

    /** 仪器得是好端端的才开得了台 —— 维修中、停用都开不出去 */
    public void assertUsable() {
        if ("维修中".equals(instrumentStatus)) {
            throw new BizException("「" + instrumentName + "」正在维修中，开不了台");
        }
        if ("停用".equals(instrumentStatus)) {
            throw new BizException("「" + instrumentName + "」已经停用，开不了台");
        }
        if (!"可用".equals(instrumentStatus)) {
            throw new BizException("「" + instrumentName + "」现在不是可用状态，开不了台");
        }
    }
}

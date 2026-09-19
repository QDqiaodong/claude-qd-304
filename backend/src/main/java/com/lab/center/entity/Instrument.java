package com.lab.center.entity;

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
}

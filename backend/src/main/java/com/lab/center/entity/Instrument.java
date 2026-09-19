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

    public boolean usable() {
        return "可用".equals(instrumentStatus);
    }

    /** 开台前先过仪器这关：维修中、停用都开不了。 */
    public void assertOpenable() {
        if (!usable()) {
            throw new BizException("「" + instrumentName + "」现在是「" + instrumentStatus + "」，开不了台");
        }
    }

    /** 仪器大类靠名字认 —— 柜型配对规则只关心这两类，其余归「其他」。 */
    public String category() {
        if (instrumentName != null && instrumentName.contains("分光光度计")) {
            return "分光光度计";
        }
        if (instrumentName != null && instrumentName.contains("恒温水浴")) {
            return "恒温水浴";
        }
        return "其他";
    }
}

package com.lab.center.entity;

import com.lab.center.dto.BizException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 试剂柜。 */
@Entity
@Table(name = "cabinet")
public class Cabinet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "cabinet_code", nullable = false, length = 24, unique = true)
    public String cabinetCode;

    @Column(name = "cabinet_name", nullable = false, length = 64)
    public String cabinetName;

    /** 通风柜 / 普通柜 / 防爆柜 */
    @Column(name = "cabinet_kind", length = 24)
    public String cabinetKind;

    @Column(name = "cabinet_status", nullable = false, length = 16)
    public String cabinetStatus;

    /** 柜子停用之前先把自己检查一遍。 */
    public void assertUsable() {
        if ("停用".equals(cabinetStatus)) {
            throw new BizException("试剂柜「" + cabinetName + "」已经停用，试剂不能往这儿放");
        }
    }
}

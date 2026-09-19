package com.lab.center.entity;

import com.lab.center.dto.BizException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 试剂 —— 这一版把业务规则直接写在实体上：
 * 「过没过期」「领用扣库存够不够」都是试剂自己的事，service 只负责编排。
 */
@Entity
@Table(name = "reagent")
public class Reagent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "reagent_code", nullable = false, length = 24, unique = true)
    public String reagentCode;

    @Column(name = "reagent_name", nullable = false, length = 64)
    public String reagentName;

    @Column(name = "spec_text", length = 32)
    public String specText;

    @Column(name = "cabinet_id")
    public Long cabinetId;

    @Column(name = "balance", nullable = false)
    public Integer balance;

    @Column(name = "expire_date")
    public LocalDate expireDate;

    @Column(name = "reagent_status", nullable = false, length = 16)
    public String reagentStatus;

    /** 有效期已经过了 */
    public boolean expired() {
        return expireDate != null && expireDate.isBefore(LocalDate.now());
    }

    /** 还能领出来吗 —— 把不能领的原因直接说清楚 */
    public void assertLendable(int qty) {
        if (qty <= 0) {
            throw new BizException("领用量得大于 0");
        }
        if (expired()) {
            throw new BizException("「" + reagentName + "」" + expireDate + " 就到期了，不能再领");
        }
        if ("停用".equals(reagentStatus)) {
            throw new BizException("「" + reagentName + "」已经停用");
        }
        if (balance == null || balance < qty) {
            throw new BizException("「" + reagentName + "」只剩 " + balance + "，领不了 " + qty);
        }
    }

    /** 领用：扣库存，扣到 0 就标成已用完。 */
    public void deduct(int qty) {
        assertLendable(qty);
        balance -= qty;
        if (balance == 0) {
            reagentStatus = "已用完";
        }
    }

    /** 退回来：库存加回去，状态也跟着回。 */
    public void refund(int qty) {
        if (qty <= 0) {
            throw new BizException("退回量得大于 0");
        }
        if (expired()) {
            throw new BizException("「" + reagentName + "」已经过期，退回来也不能再用");
        }
        balance = (balance == null ? 0 : balance) + qty;
        if ("已用完".equals(reagentStatus)) {
            reagentStatus = "可用";
        }
    }

    /**
     * 开台预占：对照试剂一上台就从现库存划出来。
     * 注意这不是领用 —— 不写领用流水，库存照样扣。
     */
    public void reserve(int qty) {
        if (qty <= 0) {
            throw new BizException("预占量得大于 0");
        }
        if (expired()) {
            throw new BizException("「" + reagentName + "」" + expireDate + " 就到期了，开不了台");
        }
        if ("停用".equals(reagentStatus)) {
            throw new BizException("「" + reagentName + "」已经停用，开不了台");
        }
        if (balance == null || balance < qty) {
            throw new BizException("「" + reagentName + "」只剩 " + balance + "，不够预占 " + qty);
        }
        balance -= qty;
        if (balance == 0) {
            reagentStatus = "已用完";
        }
    }

    /** 单作废了，预占吐回来，试剂回到还能再开的状态。 */
    public void release(int qty) {
        if (qty <= 0) {
            throw new BizException("吐回量得大于 0");
        }
        balance = (balance == null ? 0 : balance) + qty;
        if ("已用完".equals(reagentStatus)) {
            reagentStatus = "可用";
        }
    }
}

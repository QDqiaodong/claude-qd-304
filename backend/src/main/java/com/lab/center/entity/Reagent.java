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

    /** 开台预占出去的量 —— 占着位但没写领用流水，收口才真正销掉 */
    @Column(name = "reserved")
    public Integer reserved;

    @Column(name = "expire_date")
    public LocalDate expireDate;

    @Column(name = "reagent_status", nullable = false, length = 16)
    public String reagentStatus;

    /** 有效期已经过了 */
    public boolean expired() {
        return expireDate != null && expireDate.isBefore(LocalDate.now());
    }

    /** 现库存里还能动用的量：账面库存扣掉被开台预占走的 */
    public int available() {
        return (balance == null ? 0 : balance) - (reserved == null ? 0 : reserved);
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
        if (available() < qty) {
            throw new BizException("「" + reagentName + "」只剩 " + available() + "，领不了 " + qty);
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

    /** 开台预占：从现库存里占出一瓶对照试剂 —— 只占位置，不走领用流水。 */
    public void reserve() {
        if (expired()) {
            throw new BizException("「" + reagentName + "」" + expireDate + " 就到期了，开不了台");
        }
        if ("停用".equals(reagentStatus)) {
            throw new BizException("「" + reagentName + "」已经停用，开不了台");
        }
        if (available() < 1) {
            throw new BizException("「" + reagentName + "」没有能预占的库存了，开不了台");
        }
        reserved = (reserved == null ? 0 : reserved) + 1;
    }

    /** 开台作废：预占的那瓶吐回架上，试剂回到还能再开的状态。 */
    public void releaseReserved() {
        if (reserved == null || reserved <= 0) {
            throw new BizException("「" + reagentName + "」没有预占可以吐回");
        }
        reserved -= 1;
    }

    /** 开台收口：预占的这瓶真正用掉，从账面库存里销掉。 */
    public void consumeReserved() {
        releaseReserved();
        balance = (balance == null ? 0 : balance) - 1;
        if (balance == 0) {
            reagentStatus = "已用完";
        }
    }
}

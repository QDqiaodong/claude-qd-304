package com.lab.center.entity;

import com.lab.center.dto.BizException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 对照试验开台单 —— 一张单同时挂一台仪器和一瓶对照试剂：
 * 柜型配对、仪器状态两道关都过了才开得出去；
 * 开台时试剂预占库存（不写领用流水），作废整单吐回，收口才真正销掉。
 */
@Entity
@Table(name = "bench_order", uniqueConstraints =
        @UniqueConstraint(name = "uk_bench_open", columnNames = {"instrument_id", "open_flag"}))
public class BenchOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_no", nullable = false, length = 24, unique = true)
    public String orderNo;

    @Column(name = "instrument_id", nullable = false)
    public Long instrumentId;

    @Column(name = "reagent_id", nullable = false)
    public Long reagentId;

    @Column(name = "user_name", nullable = false, length = 32)
    public String userName;

    /** 一张单就占一瓶对照试剂 */
    @Column(name = "quantity", nullable = false)
    public Integer quantity;

    /** 开台中 / 已完成 / 已作废 —— 「开台中」就是未收口 */
    @Column(name = "order_status", nullable = false, length = 16)
    public String orderStatus;

    @Column(name = "open_time", nullable = false)
    public LocalDateTime openTime;

    @Column(name = "close_time")
    public LocalDateTime closeTime;

    /** 未收口时恒为 1，收口后置空 —— 配合唯一索引保证一台仪器只开着一条单 */
    @Column(name = "open_flag")
    public Integer openFlag;

    public boolean open() {
        return "开台中".equals(orderStatus);
    }

    /**
     * 柜型配对关：试剂躺在什么柜子里，决定了它不能上什么仪器。
     * 通风柜里的易挥发，会搅分光光度计的光路；防爆柜里的遇热有引爆风险，碰不得恒温水浴。
     */
    public static void assertPairable(Cabinet cabinet, Instrument instrument) {
        String kind = cabinet == null ? null : cabinet.cabinetKind;
        String iname = instrument == null ? null : instrument.instrumentName;
        if (kind == null || iname == null) {
            return;
        }
        if ("通风柜".equals(kind) && iname.contains("分光光度计")) {
            throw new BizException("「" + iname + "」配不了通风柜里的试剂：挥发会搅乱光路");
        }
        if ("防爆柜".equals(kind) && iname.contains("恒温水浴")) {
            throw new BizException("「" + iname + "」配不了防爆柜里的试剂：加热有引爆风险");
        }
    }

    /** 收口完成：仪器还得是好端端的；仪器倒了的单只能作废，不许硬收口 */
    public void complete(Instrument instrument) {
        if (!open()) {
            throw new BizException("这单已经收口了，不用再动");
        }
        if (instrument == null) {
            throw new BizException("单上的仪器找不到了，这单收不了口，只能整单作废");
        }
        if (!"可用".equals(instrument.instrumentStatus)) {
            throw new BizException("「" + instrument.instrumentName + "」已经"
                    + instrument.instrumentStatus + "，这单收不了口，只能整单作废");
        }
        orderStatus = "已完成";
        closeTime = LocalDateTime.now();
        openFlag = null;
    }

    /** 整单作废：预占的试剂得跟着吐回去 */
    public void voidOrder() {
        if (!open()) {
            throw new BizException("这单已经收口了，作废不了");
        }
        orderStatus = "已作废";
        closeTime = LocalDateTime.now();
        openFlag = null;
    }
}

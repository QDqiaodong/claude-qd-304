package com.lab.center.entity;

import com.lab.center.dto.BizException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 对照试验开台单 —— 一台仪器 + 一瓶对照试剂，缺一不可。
 * 状态机很简单：进行中 → 已完成 / 已作废，收口之后不许再动。
 * 柜型 × 仪器的配对红线也写在这里，开台和收口都绕不开它。
 */
@Entity
@Table(name = "bench_order")
public class BenchOrder {

    public static final String OPEN = "进行中";
    public static final String DONE = "已完成";
    public static final String VOID = "已作废";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_no", nullable = false, length = 24, unique = true)
    public String orderNo;

    @Column(name = "instrument_id", nullable = false)
    public Long instrumentId;

    @Column(name = "reagent_id", nullable = false)
    public Long reagentId;

    @Column(name = "operator_name", nullable = false, length = 32)
    public String operatorName;

    /** 预占的对照试剂数量，开台时从现库存划出来，不走领用流水。 */
    @Column(name = "quantity", nullable = false)
    public Integer quantity = 1;

    @Column(name = "status", nullable = false, length = 16)
    public String status;

    /** 进行中时 = instrumentId，收口置空；数据库唯一索引靠它挡住第二张开台单。 */
    @Column(name = "open_instrument_id", unique = true)
    public Long openInstrumentId;

    @Column(name = "opened_at", nullable = false)
    public LocalDateTime openedAt;

    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    /**
     * 柜型 × 仪器配对红线，两条都是硬拦：
     * 通风柜里的试剂会挥发，搅分光光度计的光路；
     * 防爆柜里的危化品上恒温水浴加热，有引爆风险。
     */
    public static void assertPairable(Cabinet cabinet, Instrument instrument) {
        if (cabinet == null || instrument == null) {
            return;
        }
        String kind = cabinet.cabinetKind;
        String category = instrument.category();
        if ("通风柜".equals(kind) && "分光光度计".equals(category)) {
            throw new BizException("「" + cabinet.cabinetName + "」里的试剂会挥发，搅 "
                    + instrument.instrumentName + " 的光路，这个台开不了");
        }
        if ("防爆柜".equals(kind) && "恒温水浴".equals(category)) {
            throw new BizException("「" + cabinet.cabinetName + "」里的试剂上 "
                    + instrument.instrumentName + " 加热有引爆风险，这个台开不了");
        }
    }

    public boolean open() {
        return OPEN.equals(status);
    }

    /**
     * 收口完成。仪器要是已经维修中 / 停用，这单就只能作废，
     * 不许点完成，也没有「换一瓶试剂硬收口」的口子 —— 单上的试剂从开台起就锁死了。
     */
    public void finish(Instrument instrument) {
        if (!open()) {
            throw new BizException("这单已经收口了，别再动它");
        }
        if (instrument != null && !instrument.usable()) {
            throw new BizException("「" + instrument.instrumentName + "」现在是「"
                    + instrument.instrumentStatus + "」，这单只能整单作废，不能完成");
        }
        status = DONE;
        openInstrumentId = null;
        closedAt = LocalDateTime.now();
    }

    /** 整单作废：预占库存由 service 吐回给试剂。 */
    public void voidIt() {
        if (!open()) {
            throw new BizException("这单已经收口了，别再动它");
        }
        status = VOID;
        openInstrumentId = null;
        closedAt = LocalDateTime.now();
    }
}

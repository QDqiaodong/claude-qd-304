package com.lab.center.service;

import com.lab.center.dto.BizException;
import com.lab.center.entity.BenchOrder;
import com.lab.center.entity.Cabinet;
import com.lab.center.entity.Instrument;
import com.lab.center.entity.Reagent;
import com.lab.center.repository.BenchOrderRepository;
import com.lab.center.repository.CabinetRepository;
import com.lab.center.repository.InstrumentRepository;
import com.lab.center.repository.ReagentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 对照试验开台：一张单要同时挂住仪器、对照试剂，柜型配对也得过关，
 * 缺任何一样台都开不出去。预占只动试剂库存，绝不写领用 / 退回流水。
 */
@Service
public class BenchOrderService {

    private final BenchOrderRepository orders;
    private final InstrumentRepository instruments;
    private final ReagentRepository reagents;
    private final CabinetRepository cabinets;

    public BenchOrderService(BenchOrderRepository orders, InstrumentRepository instruments,
                             ReagentRepository reagents, CabinetRepository cabinets) {
        this.orders = orders;
        this.instruments = instruments;
        this.reagents = reagents;
        this.cabinets = cabinets;
    }

    public List<BenchOrder> list(String status) {
        return orders.findAllByOrderByIdDesc().stream()
                .filter(o -> status == null || status.isBlank() || status.equals(o.status))
                .toList();
    }

    /**
     * 开台。整个方法一个事务：中途任何一步失败，单落不下去，库存也不会扣一半。
     * 校验顺序不影响结论 —— 仪器状态、柜型配对、试剂库存，三道关全都得过。
     */
    @Transactional
    public BenchOrder open(BenchOrder form) {
        if (form.instrumentId == null) {
            throw new BizException("开台得挂一台仪器");
        }
        if (form.reagentId == null) {
            throw new BizException("开台得锁一瓶对照试剂");
        }
        if (form.operatorName == null || form.operatorName.isBlank()) {
            throw new BizException("开台人不能空着");
        }
        Instrument instrument = instruments.findById(form.instrumentId)
                .orElseThrow(() -> new BizException("要用的仪器不存在"));
        Reagent reagent = reagents.findById(form.reagentId)
                .orElseThrow(() -> new BizException("要锁的对照试剂不存在"));

        // 仪器状态这关：维修中、停用都开不了
        instrument.assertOpenable();

        // 柜型配对这关：通风柜 × 分光光度计、防爆柜 × 恒温水浴，都拦
        Cabinet cabinet = reagent.cabinetId == null ? null
                : cabinets.findById(reagent.cabinetId).orElse(null);
        BenchOrder.assertPairable(cabinet, instrument);

        // 同一台仪器上未收口的开台只许一条，第二个人当场被挡
        if (orders.existsByInstrumentIdAndStatus(instrument.id, BenchOrder.OPEN)) {
            throw new BizException("「" + instrument.instrumentName + "」这台已经有未收口的对照试验，不能再开");
        }

        // 对照试剂从现库存预占出来 —— 只动库存，不写领用 / 退回流水
        int qty = form.quantity == null ? 1 : form.quantity;
        reagent.reserve(qty);
        reagents.save(reagent);

        form.id = null;
        form.orderNo = nextOrderNo();
        form.operatorName = form.operatorName.trim();
        form.quantity = qty;
        form.status = BenchOrder.OPEN;
        form.openInstrumentId = instrument.id;
        form.openedAt = LocalDateTime.now();
        form.closedAt = null;
        try {
            return orders.saveAndFlush(form);
        } catch (DataIntegrityViolationException e) {
            // 并发下靠唯一索引兜底：这台仪器上已经有一条未收口的单
            throw new BizException("「" + instrument.instrumentName + "」这台已经有未收口的对照试验，不能再开");
        }
    }

    /** 完成收口：仪器要是已经被改成维修中 / 停用，这单就只能作废，完成不了。 */
    @Transactional
    public BenchOrder finish(Long id) {
        BenchOrder order = orders.findById(id).orElseThrow(() -> new BizException("开台单不存在"));
        Instrument instrument = instruments.findById(order.instrumentId)
                .orElseThrow(() -> new BizException("单上的仪器不存在"));
        order.finish(instrument);
        return orders.save(order);
    }

    /** 整单作废：预占的库存吐回给试剂，试剂回到还能再开的状态。 */
    @Transactional
    public BenchOrder cancel(Long id) {
        BenchOrder order = orders.findById(id).orElseThrow(() -> new BizException("开台单不存在"));
        order.voidIt();
        Reagent reagent = reagents.findById(order.reagentId)
                .orElseThrow(() -> new BizException("单上的对照试剂不存在"));
        reagent.release(order.quantity);
        reagents.save(reagent);
        return orders.save(order);
    }

    private String nextOrderNo() {
        String no;
        do {
            no = "BO-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        } while (orders.findByOrderNo(no).isPresent());
        return no;
    }
}

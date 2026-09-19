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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开台服务同样很薄：柜型配对在 BenchOrder 自己身上，仪器状态在 Instrument 上，
 * 预占 / 吐回 / 销账在 Reagent 上 —— 这里只负责把几道关按顺序过完再落库。
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
                .filter(o -> status == null || status.isBlank() || status.equals(o.orderStatus))
                .toList();
    }

    @Transactional
    public BenchOrder open(BenchOrder form) {
        if (form.orderNo == null || form.orderNo.isBlank()) {
            throw new BizException("开台单号不能空着");
        }
        form.orderNo = form.orderNo.trim();
        if (orders.findByOrderNo(form.orderNo).isPresent()) {
            throw new BizException("开台单号 " + form.orderNo + " 已经用过了");
        }
        if (form.instrumentId == null) {
            throw new BizException("得挑一台仪器");
        }
        if (form.reagentId == null) {
            throw new BizException("得挑一瓶对照试剂");
        }
        if (form.userName == null || form.userName.isBlank()) {
            throw new BizException("开台人不能空着");
        }
        Instrument inst = instruments.findById(form.instrumentId)
                .orElseThrow(() -> new BizException("要开台的仪器不存在"));
        Reagent reagent = reagents.findById(form.reagentId)
                .orElseThrow(() -> new BizException("要开台的对照试剂不存在"));
        Cabinet cab = reagent.cabinetId == null ? null
                : cabinets.findById(reagent.cabinetId).orElse(null);

        // 两道关一道都不能少：仪器状态要过，柜型配对也要过
        inst.assertUsable();
        BenchOrder.assertPairable(cab, inst);
        if (orders.existsByInstrumentIdAndOrderStatus(inst.id, "开台中")) {
            throw new BizException("「" + inst.instrumentName + "」已经有未收口的对照试验，不能再开一台");
        }

        // 预占一瓶：只占位置，不写领用 / 退回流水
        reagent.reserve();
        reagents.save(reagent);

        form.userName = form.userName.trim();
        form.quantity = 1;
        form.orderStatus = "开台中";
        form.openTime = LocalDateTime.now();
        form.closeTime = null;
        form.openFlag = 1;
        return orders.save(form);
    }

    /** 收口：仪器还得好端端的才收得了；预占的那瓶真正销掉。 */
    @Transactional
    public BenchOrder complete(Long id) {
        BenchOrder o = orders.findById(id).orElseThrow(() -> new BizException("这张开台单不存在"));
        Instrument inst = instruments.findById(o.instrumentId).orElse(null);
        o.complete(inst);
        Reagent r = reagents.findById(o.reagentId)
                .orElseThrow(() -> new BizException("单上的对照试剂不存在"));
        r.consumeReserved();
        reagents.save(r);
        return orders.save(o);
    }

    /** 整单作废：预占吐回，试剂回到还能再开的状态。 */
    @Transactional
    public BenchOrder voidOrder(Long id) {
        BenchOrder o = orders.findById(id).orElseThrow(() -> new BizException("这张开台单不存在"));
        o.voidOrder();
        Reagent r = reagents.findById(o.reagentId)
                .orElseThrow(() -> new BizException("单上的对照试剂不存在"));
        r.releaseReserved();
        reagents.save(r);
        return orders.save(o);
    }
}

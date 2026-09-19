package com.lab.center;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lab.center.dto.BizException;
import com.lab.center.entity.BenchOrder;
import com.lab.center.entity.Cabinet;
import com.lab.center.entity.Instrument;
import com.lab.center.entity.Reagent;
import com.lab.center.repository.BenchOrderRepository;
import com.lab.center.repository.CabinetRepository;
import com.lab.center.repository.InstrumentRepository;
import com.lab.center.repository.ReagentRepository;
import com.lab.center.repository.UsageLogRepository;
import com.lab.center.service.BenchOrderService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 对照试验开台的验收点，一条条跑给后端看。 */
@SpringBootTest
class BenchOrderServiceTest {

    @Autowired
    BenchOrderService service;
    @Autowired
    BenchOrderRepository orders;
    @Autowired
    InstrumentRepository instruments;
    @Autowired
    ReagentRepository reagents;
    @Autowired
    CabinetRepository cabinets;
    @Autowired
    UsageLogRepository usageLogs;

    private Cabinet fumeHood;
    private Cabinet bombBox;
    private Cabinet plain;
    private Instrument spectro;
    private Instrument waterBath;
    private Instrument scale;

    @BeforeEach
    void setUp() {
        orders.deleteAll();
        usageLogs.deleteAll();
        reagents.deleteAll();
        instruments.deleteAll();
        cabinets.deleteAll();

        fumeHood = cabinets.save(cabinet("CB-1", "通风柜 A", "通风柜"));
        bombBox = cabinets.save(cabinet("CB-2", "防爆柜 C", "防爆柜"));
        plain = cabinets.save(cabinet("CB-3", "普通柜 B", "普通柜"));

        spectro = instruments.save(instrument("IN-1", "紫外分光光度计", "可用"));
        waterBath = instruments.save(instrument("IN-2", "恒温水浴锅", "可用"));
        scale = instruments.save(instrument("IN-3", "电子天平", "可用"));
    }

    private Cabinet cabinet(String code, String name, String kind) {
        Cabinet c = new Cabinet();
        c.cabinetCode = code;
        c.cabinetName = name;
        c.cabinetKind = kind;
        c.cabinetStatus = "可用";
        return c;
    }

    private Instrument instrument(String code, String name, String status) {
        Instrument i = new Instrument();
        i.instrumentCode = code;
        i.instrumentName = name;
        i.instrumentStatus = status;
        return i;
    }

    private Reagent reagent(String code, Cabinet cabinet, int balance) {
        Reagent r = new Reagent();
        r.reagentCode = code;
        r.reagentName = "试剂" + code;
        r.cabinetId = cabinet.id;
        r.balance = balance;
        r.expireDate = LocalDate.now().plusYears(1);
        r.reagentStatus = "可用";
        return reagents.save(r);
    }

    private BenchOrder form(Long instrumentId, Long reagentId) {
        BenchOrder f = new BenchOrder();
        f.instrumentId = instrumentId;
        f.reagentId = reagentId;
        f.operatorName = "张三";
        return f;
    }

    @Test
    void 开台成功_库存预占但不写领用流水() {
        Reagent r = reagent("RG-1", plain, 5);
        BenchOrder o = service.open(form(scale.id, r.id));

        assertEquals(BenchOrder.OPEN, o.status);
        assertEquals(scale.id, o.openInstrumentId);
        assertEquals(4, reagents.findById(r.id).orElseThrow().balance.intValue());
        assertEquals(0, usageLogs.count());
    }

    @Test
    void 只挂仪器或只锁试剂都开不出去() {
        Reagent r = reagent("RG-1", plain, 5);
        assertThrows(BizException.class, () -> service.open(form(null, r.id)));
        assertThrows(BizException.class, () -> service.open(form(scale.id, null)));
        assertEquals(0, orders.count());
        assertEquals(5, reagents.findById(r.id).orElseThrow().balance.intValue());
    }

    @Test
    void 通风柜试剂配分光光度计要拦() {
        Reagent r = reagent("RG-1", fumeHood, 5);
        BizException e = assertThrows(BizException.class, () -> service.open(form(spectro.id, r.id)));
        assertTrue(e.getMessage().contains("光路"));
        assertEquals(0, orders.count());
        assertEquals(5, reagents.findById(r.id).orElseThrow().balance.intValue());
    }

    @Test
    void 防爆柜试剂配恒温水浴要拦() {
        Reagent r = reagent("RG-1", bombBox, 5);
        BizException e = assertThrows(BizException.class, () -> service.open(form(waterBath.id, r.id)));
        assertTrue(e.getMessage().contains("引爆"));
        assertEquals(0, orders.count());
    }

    @Test
    void 柜型没问题的组合照样能开() {
        Reagent r = reagent("RG-1", plain, 5);
        assertEquals(BenchOrder.OPEN, service.open(form(spectro.id, r.id)).status);
        Reagent r2 = reagent("RG-2", fumeHood, 5);
        assertEquals(BenchOrder.OPEN, service.open(form(waterBath.id, r2.id)).status);
    }

    @Test
    void 仪器维修中或停用都开不了() {
        Instrument fixing = instruments.save(instrument("IN-9", "离心机", "维修中"));
        Instrument dead = instruments.save(instrument("IN-10", "马弗炉", "停用"));
        Reagent r = reagent("RG-1", plain, 5);
        assertThrows(BizException.class, () -> service.open(form(fixing.id, r.id)));
        assertThrows(BizException.class, () -> service.open(form(dead.id, r.id)));
        assertEquals(0, orders.count());
    }

    @Test
    void 同一台仪器未收口只许一条() {
        Reagent r1 = reagent("RG-1", plain, 5);
        Reagent r2 = reagent("RG-2", plain, 5);
        service.open(form(scale.id, r1.id));

        BizException e = assertThrows(BizException.class, () -> service.open(form(scale.id, r2.id)));
        assertTrue(e.getMessage().contains("这台已经有未收口的对照试验"));
        assertEquals(1, orders.count());
        // 第二瓶试剂一点都没被预占
        assertEquals(5, reagents.findById(r2.id).orElseThrow().balance.intValue());
    }

    @Test
    void 完成收口_预占不吐回_仪器可以接着开() {
        Reagent r = reagent("RG-1", plain, 5);
        BenchOrder o = service.open(form(scale.id, r.id));
        BenchOrder done = service.finish(o.id);

        assertEquals(BenchOrder.DONE, done.status);
        assertNull(done.openInstrumentId);
        assertEquals(4, reagents.findById(r.id).orElseThrow().balance.intValue());
        // 仪器空出来了，下一单能开
        assertEquals(BenchOrder.OPEN, service.open(form(scale.id, r.id)).status);
    }

    @Test
    void 作废吐回预占_试剂回到还能再开() {
        Reagent r = reagent("RG-1", plain, 1);
        BenchOrder o = service.open(form(scale.id, r.id));
        assertEquals("已用完", reagents.findById(r.id).orElseThrow().reagentStatus);

        BenchOrder voided = service.cancel(o.id);
        assertEquals(BenchOrder.VOID, voided.status);
        Reagent after = reagents.findById(r.id).orElseThrow();
        assertEquals(1, after.balance.intValue());
        assertEquals("可用", after.reagentStatus);
        assertEquals(0, usageLogs.count());
        // 同一台仪器、同一瓶试剂，能再开
        assertEquals(BenchOrder.OPEN, service.open(form(scale.id, r.id)).status);
    }

    @Test
    void 仪器中途趴窝_只能作废不能完成() {
        Reagent r = reagent("RG-1", plain, 5);
        BenchOrder o = service.open(form(scale.id, r.id));

        Instrument down = instruments.findById(scale.id).orElseThrow();
        down.instrumentStatus = "维修中";
        instruments.save(down);

        BizException e = assertThrows(BizException.class, () -> service.finish(o.id));
        assertTrue(e.getMessage().contains("只能整单作废"));
        // 单还没收口，完成不了；作废把预占吐回去
        assertEquals(BenchOrder.OPEN, orders.findById(o.id).orElseThrow().status);
        service.cancel(o.id);
        assertEquals(5, reagents.findById(r.id).orElseThrow().balance.intValue());
    }

    @Test
    void 收过口的单不能再动() {
        Reagent r = reagent("RG-1", plain, 5);
        BenchOrder o = service.open(form(scale.id, r.id));
        service.finish(o.id);
        assertThrows(BizException.class, () -> service.finish(o.id));
        assertThrows(BizException.class, () -> service.cancel(o.id));
    }
}

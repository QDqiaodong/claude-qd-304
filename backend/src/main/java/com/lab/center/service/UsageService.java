package com.lab.center.service;

import com.lab.center.dto.BizException;
import com.lab.center.entity.Reagent;
import com.lab.center.entity.UsageLog;
import com.lab.center.repository.ReagentRepository;
import com.lab.center.repository.UsageLogRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 领用与退回：库存怎么动，全交给 Reagent 自己去算。 */
@Service
public class UsageService {

    private final UsageLogRepository logs;
    private final ReagentRepository reagents;

    public UsageService(UsageLogRepository logs, ReagentRepository reagents) {
        this.logs = logs;
        this.reagents = reagents;
    }

    public List<UsageLog> list(Long reagentId, String direction) {
        List<UsageLog> all = reagentId == null
                ? logs.findAllByOrderByIdDesc()
                : logs.findByReagentIdOrderByIdDesc(reagentId);
        return all.stream()
                .filter(l -> direction == null || direction.isBlank() || direction.equals(l.direction))
                .toList();
    }

    @Transactional
    public UsageLog record(UsageLog form) {
        if (form.logNo == null || form.logNo.isBlank()) {
            throw new BizException("登记单号不能空着");
        }
        form.logNo = form.logNo.trim();
        if (logs.findByLogNo(form.logNo).isPresent()) {
            throw new BizException("登记单号 " + form.logNo + " 已经用过了");
        }
        if (form.reagentId == null) {
            throw new BizException("得指明领的是哪瓶试剂");
        }
        if (!"领用".equals(form.direction) && !"退回".equals(form.direction)) {
            throw new BizException("方向只能是领用或者退回");
        }
        if (form.userName == null || form.userName.isBlank()) {
            throw new BizException("领用人不能空着");
        }
        Reagent r = reagents.findById(form.reagentId).orElseThrow(() -> new BizException("要领的试剂不存在"));

        int qty = form.quantity == null ? 0 : form.quantity;
        if ("领用".equals(form.direction)) {
            r.deduct(qty);
        } else {
            r.refund(qty);
        }
        reagents.save(r);

        form.quantity = qty;
        form.useDate = form.useDate == null ? LocalDate.now() : form.useDate;
        form.userName = form.userName.trim();
        return logs.save(form);
    }
}

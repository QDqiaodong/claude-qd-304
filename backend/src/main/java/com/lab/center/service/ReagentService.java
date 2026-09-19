package com.lab.center.service;

import com.lab.center.dto.BizException;
import com.lab.center.entity.Cabinet;
import com.lab.center.entity.Reagent;
import com.lab.center.repository.CabinetRepository;
import com.lab.center.repository.ReagentRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 试剂服务很薄 —— 过期判断、库存够不够这些都在 Reagent 实体自己身上，
 * 这里只做「取数据 + 编排 + 落库」。
 */
@Service
public class ReagentService {

    private final ReagentRepository reagents;
    private final CabinetRepository cabinets;

    public ReagentService(ReagentRepository reagents, CabinetRepository cabinets) {
        this.reagents = reagents;
        this.cabinets = cabinets;
    }

    public List<Reagent> list(Long cabinetId, String status, String keyword) {
        return reagents.findAllByOrderByIdAsc().stream()
                .filter(r -> cabinetId == null || cabinetId.equals(r.cabinetId))
                .filter(r -> status == null || status.isBlank() || status.equals(r.reagentStatus))
                .filter(r -> keyword == null || keyword.isBlank()
                        || r.reagentCode.contains(keyword) || r.reagentName.contains(keyword))
                .toList();
    }

    public List<Reagent> expiring(int days) {
        LocalDate limit = LocalDate.now().plusDays(days);
        return reagents.findAllByOrderByIdAsc().stream()
                .filter(r -> r.expireDate != null && !r.expired() && !r.expireDate.isAfter(limit))
                .toList();
    }

    @Transactional
    public Reagent save(Reagent form) {
        Reagent origin = null;
        if (form.id != null) {
            origin = reagents.findById(form.id).orElseThrow(() -> new BizException("这瓶试剂不存在"));
            if (form.reagentCode == null || form.reagentCode.isBlank()) {
                form.reagentCode = origin.reagentCode;
            }
            if (form.reagentName == null || form.reagentName.isBlank()) {
                form.reagentName = origin.reagentName;
            }
        }
        if (form.reagentCode == null || form.reagentCode.isBlank()) {
            throw new BizException("试剂编号不能空着");
        }
        form.reagentCode = form.reagentCode.trim();
        reagents.findByReagentCode(form.reagentCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.reagentCode + " 已经贴在别的试剂上了");
            }
        });
        if (form.cabinetId != null) {
            Cabinet cab = cabinets.findById(form.cabinetId).orElseThrow(() -> new BizException("要放的试剂柜不存在"));
            cab.assertUsable();
        }
        if (origin == null) {
            form.balance = form.balance == null ? 0 : form.balance;
            form.reagentStatus = form.reagentStatus == null || form.reagentStatus.isBlank() ? "可用" : form.reagentStatus;
            return reagents.save(form);
        }
        // 库存只跟着领用/退回走，不在这里改
        if (form.specText != null && !form.specText.isBlank()) {
            origin.specText = form.specText;
        }
        if (form.expireDate != null) {
            origin.expireDate = form.expireDate;
        }
        if (form.cabinetId != null) {
            origin.cabinetId = form.cabinetId;
        }
        if (form.reagentStatus != null && !form.reagentStatus.isBlank()) {
            origin.reagentStatus = form.reagentStatus;
        }
        origin.reagentCode = form.reagentCode;
        origin.reagentName = form.reagentName;
        return reagents.save(origin);
    }
}

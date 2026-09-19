package com.lab.center.service;

import com.lab.center.dto.BizException;
import com.lab.center.entity.Cabinet;
import com.lab.center.repository.CabinetRepository;
import com.lab.center.repository.ReagentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CabinetService {

    private final CabinetRepository cabinets;
    private final ReagentRepository reagents;

    public CabinetService(CabinetRepository cabinets, ReagentRepository reagents) {
        this.cabinets = cabinets;
        this.reagents = reagents;
    }

    public List<Cabinet> list(String status, String keyword) {
        return cabinets.findAllByOrderByIdAsc().stream()
                .filter(c -> status == null || status.isBlank() || status.equals(c.cabinetStatus))
                .filter(c -> keyword == null || keyword.isBlank()
                        || c.cabinetCode.contains(keyword) || c.cabinetName.contains(keyword))
                .toList();
    }

    @Transactional
    public Cabinet save(Cabinet form) {
        Cabinet origin = null;
        if (form.id != null) {
            origin = cabinets.findById(form.id).orElseThrow(() -> new BizException("试剂柜不存在"));
            if (form.cabinetCode == null || form.cabinetCode.isBlank()) {
                form.cabinetCode = origin.cabinetCode;
            }
            if (form.cabinetName == null || form.cabinetName.isBlank()) {
                form.cabinetName = origin.cabinetName;
            }
        }
        if (form.cabinetCode == null || form.cabinetCode.isBlank()) {
            throw new BizException("试剂柜编号不能空着");
        }
        if (form.cabinetName == null || form.cabinetName.isBlank()) {
            throw new BizException("试剂柜名称不能空着");
        }
        form.cabinetCode = form.cabinetCode.trim();
        cabinets.findByCabinetCode(form.cabinetCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.cabinetCode + " 已经用在别的柜子上了");
            }
        });
        if (origin == null) {
            form.cabinetStatus = form.cabinetStatus == null || form.cabinetStatus.isBlank() ? "可用" : form.cabinetStatus;
            return cabinets.save(form);
        }
        if ("停用".equals(form.cabinetStatus) && !"停用".equals(origin.cabinetStatus)
                && reagents.countByCabinetId(form.id) > 0) {
            throw new BizException("柜子里还放着试剂，先清空再停用");
        }
        if (form.cabinetKind != null && !form.cabinetKind.isBlank()) {
            origin.cabinetKind = form.cabinetKind;
        }
        if (form.cabinetStatus != null && !form.cabinetStatus.isBlank()) {
            origin.cabinetStatus = form.cabinetStatus;
        }
        origin.cabinetCode = form.cabinetCode;
        origin.cabinetName = form.cabinetName;
        return cabinets.save(origin);
    }
}

package com.lab.center.service;

import com.lab.center.dto.BizException;
import com.lab.center.entity.Instrument;
import com.lab.center.repository.InstrumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentService {

    private final InstrumentRepository instruments;

    public InstrumentService(InstrumentRepository instruments) {
        this.instruments = instruments;
    }

    public List<Instrument> list(String status, String keyword) {
        return instruments.findAllByOrderByIdAsc().stream()
                .filter(i -> status == null || status.isBlank() || status.equals(i.instrumentStatus))
                .filter(i -> keyword == null || keyword.isBlank()
                        || i.instrumentCode.contains(keyword) || i.instrumentName.contains(keyword))
                .toList();
    }

    @Transactional
    public Instrument save(Instrument form) {
        Instrument origin = null;
        if (form.id != null) {
            origin = instruments.findById(form.id).orElseThrow(() -> new BizException("仪器不存在"));
            if (form.instrumentCode == null || form.instrumentCode.isBlank()) {
                form.instrumentCode = origin.instrumentCode;
            }
            if (form.instrumentName == null || form.instrumentName.isBlank()) {
                form.instrumentName = origin.instrumentName;
            }
        }
        if (form.instrumentCode == null || form.instrumentCode.isBlank()) {
            throw new BizException("仪器编号不能空着");
        }
        if (form.instrumentName == null || form.instrumentName.isBlank()) {
            throw new BizException("仪器名称不能空着");
        }
        form.instrumentCode = form.instrumentCode.trim();
        instruments.findByInstrumentCode(form.instrumentCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.instrumentCode + " 已经用在别的仪器上了");
            }
        });
        if (origin == null) {
            form.instrumentStatus = form.instrumentStatus == null || form.instrumentStatus.isBlank()
                    ? "可用" : form.instrumentStatus;
            return instruments.save(form);
        }
        if (form.modelText != null && !form.modelText.isBlank()) {
            origin.modelText = form.modelText;
        }
        if (form.keeper != null && !form.keeper.isBlank()) {
            origin.keeper = form.keeper;
        }
        if (form.instrumentStatus != null && !form.instrumentStatus.isBlank()) {
            origin.instrumentStatus = form.instrumentStatus;
        }
        origin.instrumentCode = form.instrumentCode;
        origin.instrumentName = form.instrumentName;
        return instruments.save(origin);
    }
}

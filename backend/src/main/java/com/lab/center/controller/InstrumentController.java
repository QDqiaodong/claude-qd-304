package com.lab.center.controller;

import com.lab.center.entity.Instrument;
import com.lab.center.service.InstrumentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService service;

    public InstrumentController(InstrumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Instrument> list(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String keyword) {
        return service.list(status, keyword);
    }

    @PostMapping
    public Instrument create(@RequestBody Instrument form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Instrument update(@PathVariable Long id, @RequestBody Instrument form) {
        form.id = id;
        return service.save(form);
    }
}

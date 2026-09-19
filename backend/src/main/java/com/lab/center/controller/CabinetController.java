package com.lab.center.controller;

import com.lab.center.entity.Cabinet;
import com.lab.center.service.CabinetService;
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
@RequestMapping("/api/cabinets")
public class CabinetController {

    private final CabinetService service;

    public CabinetController(CabinetService service) {
        this.service = service;
    }

    @GetMapping
    public List<Cabinet> list(@RequestParam(required = false) String status,
                         @RequestParam(required = false) String keyword) {
        return service.list(status, keyword);
    }

    @PostMapping
    public Cabinet create(@RequestBody Cabinet form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Cabinet update(@PathVariable Long id, @RequestBody Cabinet form) {
        form.id = id;
        return service.save(form);
    }
}

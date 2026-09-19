package com.lab.center.controller;

import com.lab.center.entity.Reagent;
import com.lab.center.service.ReagentService;
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
@RequestMapping("/api/reagents")
public class ReagentController {

    private final ReagentService service;

    public ReagentController(ReagentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Reagent> list(@RequestParam(required = false) Long cabinetId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword) {
        return service.list(cabinetId, status, keyword);
    }

    /** 临期清单：默认看 30 天内要过期的 */
    @GetMapping("/expiring")
    public List<Reagent> expiring(@RequestParam(defaultValue = "30") int days) {
        return service.expiring(days);
    }

    @PostMapping
    public Reagent create(@RequestBody Reagent form) {
        return service.save(form);
    }

    @PutMapping("/{id}")
    public Reagent update(@PathVariable Long id, @RequestBody Reagent form) {
        form.id = id;
        return service.save(form);
    }
}

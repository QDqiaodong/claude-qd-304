package com.lab.center.controller;

import com.lab.center.entity.UsageLog;
import com.lab.center.service.UsageService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usages")
public class UsageController {

    private final UsageService service;

    public UsageController(UsageService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsageLog> list(@RequestParam(required = false) Long reagentId,
                               @RequestParam(required = false) String direction) {
        return service.list(reagentId, direction);
    }

    @PostMapping
    public UsageLog create(@RequestBody UsageLog form) {
        return service.record(form);
    }
}

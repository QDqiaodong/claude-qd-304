package com.lab.center.controller;

import com.lab.center.entity.BenchOrder;
import com.lab.center.service.BenchOrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bench-orders")
public class BenchOrderController {

    private final BenchOrderService service;

    public BenchOrderController(BenchOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<BenchOrder> list(@RequestParam(required = false) String status) {
        return service.list(status);
    }

    @PostMapping
    public BenchOrder open(@RequestBody BenchOrder form) {
        return service.open(form);
    }

    @PostMapping("/{id}/finish")
    public BenchOrder finish(@PathVariable Long id) {
        return service.finish(id);
    }

    @PostMapping("/{id}/cancel")
    public BenchOrder cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

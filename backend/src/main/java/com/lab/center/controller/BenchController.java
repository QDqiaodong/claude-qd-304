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
@RequestMapping("/api/benches")
public class BenchController {

    private final BenchOrderService service;

    public BenchController(BenchOrderService service) {
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

    @PostMapping("/{id}/complete")
    public BenchOrder complete(@PathVariable Long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/void")
    public BenchOrder voidOrder(@PathVariable Long id) {
        return service.voidOrder(id);
    }
}

package com.example.demo1.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class HelloController {

    @GetMapping({"/", "/hello"})
    public String sayHello(@RequestParam(required = false) String param) {
        if (param == null || param.trim().isEmpty()) {
            param = "World";
        }
        return "Hello " + param;
    }
}
package com.llmledger.controller;

import com.llmledger.dto.RootResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public RootResponse root() {
        return new RootResponse("online", "LLM 个人智能记账系统后端已启动");
    }
}

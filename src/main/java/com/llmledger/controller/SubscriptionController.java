package com.llmledger.controller;

import com.llmledger.dto.MessageResponse;
import com.llmledger.dto.SubscriptionCreateRequest;
import com.llmledger.dto.SubscriptionResponse;
import com.llmledger.entity.Subscription;
import com.llmledger.entity.User;
import com.llmledger.service.DtoMapper;
import com.llmledger.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final DtoMapper mapper;

    public SubscriptionController(SubscriptionService subscriptionService, DtoMapper mapper) {
        this.subscriptionService = subscriptionService;
        this.mapper = mapper;
    }

    @PostMapping
    public SubscriptionResponse create(@RequestBody SubscriptionCreateRequest request,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Subscription sub = subscriptionService.create(request, user);
        return mapper.toSubscriptionResponse(sub);
    }

    @GetMapping
    public List<SubscriptionResponse> list(@RequestParam(defaultValue = "0") int skip,
                                           @RequestParam(defaultValue = "100") int limit,
                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return subscriptionService.list(user, skip, limit).stream().map(mapper::toSubscriptionResponse).toList();
    }

    @DeleteMapping("/{subId}")
    public MessageResponse delete(@PathVariable Long subId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        subscriptionService.delete(user, subId);
        return new MessageResponse("success", "Deleted subscription " + subId);
    }

    @PutMapping("/{subId}/toggle")
    public SubscriptionResponse toggle(@PathVariable Long subId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return mapper.toSubscriptionResponse(subscriptionService.toggle(user, subId));
    }
}

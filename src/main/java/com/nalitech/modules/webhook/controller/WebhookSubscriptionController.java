package com.nalitech.modules.webhook.controller;

import com.nalitech.modules.webhook.dto.WebhookDtos.CreateSubscriptionRequest;
import com.nalitech.modules.webhook.dto.WebhookDtos.SubscriptionResponse;
import com.nalitech.modules.webhook.entity.WebhookDelivery;
import com.nalitech.modules.webhook.service.WebhookSubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/subscriptions")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionService service;

    public WebhookSubscriptionController(WebhookSubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubscriptionResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void test(@PathVariable UUID id) {
        service.sendTest(id);
    }

    @GetMapping("/deliveries")
    public Page<WebhookDelivery> deliveries(@RequestParam UUID subscriptionId, Pageable pageable) {
        return service.deliveries(subscriptionId, pageable);
    }
}

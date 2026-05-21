package com.llmledger.service;

import com.llmledger.dto.SubscriptionCreateRequest;
import com.llmledger.entity.Subscription;
import com.llmledger.entity.User;
import com.llmledger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription create(SubscriptionCreateRequest request, User owner) {
        Subscription sub = new Subscription();
        sub.setName(request.getName());
        sub.setAmount(Math.abs(request.getAmount()));
        sub.setCycle(request.getCycle());
        sub.setNextBillingDate(request.getNext_billing_date().toLocalDateTime());
        sub.setCategory(request.getCategory());
        sub.setIsActive(request.getIs_active() == null ? Boolean.TRUE : request.getIs_active());
        sub.setOwner(owner);
        return subscriptionRepository.save(sub);
    }

    public List<Subscription> list(User owner, int skip, int limit) {
        List<Subscription> list = subscriptionRepository.findByOwnerIdOrderByNextBillingDateAsc(owner.getId());
        int from = Math.min(skip, list.size());
        int to = Math.min(skip + limit, list.size());
        return list.subList(from, to);
    }

    public void delete(User owner, Long id) {
        Optional<Subscription> sub = subscriptionRepository.findById(id);
        if (sub.isEmpty() || !sub.get().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Subscription not found");
        }
        subscriptionRepository.delete(sub.get());
    }

    public Subscription toggle(User owner, Long id) {
        Optional<Subscription> sub = subscriptionRepository.findById(id);
        if (sub.isEmpty() || !sub.get().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Subscription not found");
        }
        Subscription s = sub.get();
        s.setIsActive(!Boolean.TRUE.equals(s.getIsActive()));
        return subscriptionRepository.save(s);
    }
}

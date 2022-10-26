package com.jetbulb.interview;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> findAll();
    List<Order> findLast100();
    Optional<Order> findById(long id);
    void add(Order order);
    void remove(Order order);
}

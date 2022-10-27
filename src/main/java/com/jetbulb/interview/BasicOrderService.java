package com.jetbulb.interview;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BasicOrderService implements OrderService {

    private static final Object EMPTY_VALUE = new Object();

    Map<Long, Order> orders = new HashMap<>();
    NavigableMap<Long, Object> linkedKeys = new TreeMap<>(new OrderFollowingComparator(orders));
    LinkedList<Order> latest = new LinkedList<>();

    ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public void add(Order order) {
        try {
            rwLock.writeLock().lock();

            orders.put(order.id(), order);
            if (latest.size() == 100) latest.removeFirst();
            latest.addLast(order);
            linkedKeys.put(order.id(), EMPTY_VALUE);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void remove(Order order) {
        try {
            rwLock.writeLock().lock();

            orders.remove(order.id());

            final var next = linkedKeys.higherKey(order.id());
            final var prev = linkedKeys.lowerKey(order.id());
            linkedKeys.remove(order.id());

            if (latest.remove(order)) {
                Long link = findLink(next, prev);
                if (link != null) {
                    latest.add(orders.get(link));
                    latest.sort(Comparator.comparingLong(o -> o.createdAt().toEpochMilli()));
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private Long findLink(Long prev, Long next) {
        if (next != null) return next;
        return prev;
    }

    @Override
    public List<Order> findAll() {
        try {
            rwLock.readLock().lock();
            return new ArrayList<>(orders.values());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Order> findLast100() {
        try {
            rwLock.readLock().lock();
            return new ArrayList<>(latest);
        } finally {
            rwLock.readLock().unlock();
        }

    }

    @Override
    public Optional<Order> findById(long id) {
        try {
            rwLock.readLock().lock();
            return Optional.ofNullable(orders.get(id));
        } finally {
            rwLock.readLock().unlock();
        }

    }

    private static class OrderFollowingComparator implements Comparator<Long> {

        final Map<Long, Order> targetOrders;

        public OrderFollowingComparator(Map<Long, Order> targetOrders) {
            this.targetOrders = targetOrders;
        }

        @Override
        public int compare(Long orderId1, Long orderId2) {
            return Long.compare(
                    targetOrders.get(orderId1).createdAt().toEpochMilli(),
                    targetOrders.get(orderId2).createdAt().toEpochMilli()
            );
        }
    }
}

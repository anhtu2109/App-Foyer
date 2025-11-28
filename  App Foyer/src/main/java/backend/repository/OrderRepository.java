package backend.repository;

import backend.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findAll();

    Optional<Order> findById(long id);

    long create(Order order);

    void update(Order order);

    void delete(long id);
}

package backend.repository;

import backend.entity.Dish;

import java.util.List;
import java.util.Optional;

public interface DishRepository {
    List<Dish> findAll();

    Optional<Dish> findById(long id);

    long save(Dish dish);

    void update(Dish dish);

    void delete(long id);
}

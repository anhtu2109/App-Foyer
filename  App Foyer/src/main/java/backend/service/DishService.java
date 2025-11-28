package backend.service;

import backend.dto.DishRequestDTO;
import backend.entity.Dish;
import backend.repository.DishRepository;

import java.util.List;
import java.util.Optional;

public class DishService {
    private final DishRepository repository;

    public DishService(DishRepository repository) {
        this.repository = repository;
    }

    public List<Dish> getMenu() {
        return repository.findAll();
    }

    public Optional<Dish> findDish(long id) {
        return repository.findById(id);
    }

    public long createDish(DishRequestDTO request) {
        Dish dish = new Dish(request.getName(), request.getPrice(), request.getCategory());
        return repository.save(dish);
    }

    public void updateDish(Dish dish) {
        repository.update(dish);
    }

    public void deleteDish(long id) {
        repository.delete(id);
    }
}

package backend.controller;

import backend.dto.DishRequestDTO;
import backend.entity.Dish;
import backend.service.DishService;

import java.util.List;
import java.util.Optional;

public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    public List<Dish> getMenu() {
        return dishService.getMenu();
    }

    public Optional<Dish> getDish(long id) {
        return dishService.findDish(id);
    }

    public long addDish(DishRequestDTO request) {
        return dishService.createDish(request);
    }

    public void updateDish(Dish dish) {
        dishService.updateDish(dish);
    }

    public void deleteDish(long id) {
        dishService.deleteDish(id);
    }
}

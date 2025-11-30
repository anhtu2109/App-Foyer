package backend.controller;

import backend.dto.DishRequestDTO;
import backend.dto.DishResponseDTO;
import backend.entity.Dish;
import backend.service.DishService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    public List<DishResponseDTO> getMenu() {
        return dishService.getMenu().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<DishResponseDTO> getDish(long id) {
        return dishService.findDish(id).map(this::mapToResponse);
    }

    public long addDish(DishRequestDTO request) {
        return dishService.createDish(request);
    }

    public void updateDish(DishResponseDTO dto) {
        Dish dish = new Dish();
        dish.setId(dto.getId());
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setCategory(dto.getCategory());
        dishService.updateDish(dish);
    }

    public void deleteDish(long id) {
        dishService.deleteDish(id);
    }

    private DishResponseDTO mapToResponse(Dish dish) {
        DishResponseDTO dto = new DishResponseDTO();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setPrice(dish.getPrice());
        dto.setCategory(dish.getCategory());
        return dto;
    }
}

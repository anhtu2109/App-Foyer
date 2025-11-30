package backend.controller;

import backend.dto.DishRequestDTO;
import backend.dto.DishResponseDTO;
import backend.service.DishService;

import java.util.List;
import java.util.Optional;

public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    public List<DishResponseDTO> getMenu() {
        return dishService.getMenu();
    }

    public Optional<DishResponseDTO> getDish(long id) {
        return dishService.findDish(id);
    }

    public long addDish(DishRequestDTO request) {
        return dishService.createDish(request);
    }

    public void updateDish(DishResponseDTO dto) {
        dishService.updateDish(dto);
    }

    public void deleteDish(long id) {
        dishService.deleteDish(id);
    }
}

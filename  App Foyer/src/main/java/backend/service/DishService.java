package backend.service;

import backend.dto.DishRequestDTO;
import backend.dto.DishResponseDTO;
import backend.entity.Dish;
import backend.repository.DishRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DishService {
    private final DishRepository repository;

    public DishService(DishRepository repository) {
        this.repository = repository;
    }

    public List<DishResponseDTO> getMenu() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Optional<DishResponseDTO> findDish(long id) {
        return repository.findById(id).map(this::mapToResponse);
    }

    public long createDish(DishRequestDTO request) {
        Dish dish = new Dish(request.getName(), request.getPrice(), request.getCategory());
        return repository.save(dish);
    }

    public void updateDish(DishResponseDTO dto) {
        Dish dish = mapToEntity(dto);
        repository.update(dish);
    }

    public void deleteDish(long id) {
        repository.delete(id);
    }

    private DishResponseDTO mapToResponse(Dish dish) {
        DishResponseDTO dto = new DishResponseDTO();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setPrice(dish.getPrice());
        dto.setCategory(dish.getCategory());
        return dto;
    }

    private Dish mapToEntity(DishResponseDTO dto) {
        Dish dish = new Dish();
        dish.setId(dto.getId());
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setCategory(dto.getCategory());
        return dish;
    }
}

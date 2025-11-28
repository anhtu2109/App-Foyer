package backend.repository.impl;

import backend.config.DatabaseConfig;
import backend.entity.Dish;
import backend.entity.DishCategory;
import backend.repository.DishRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DishRepositoryImpl implements DishRepository {
    @Override
    public List<Dish> findAll() {
        List<Dish> dishes = new ArrayList<>();
        String query = "SELECT id, name, price, category FROM dishes ORDER BY name";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                dishes.add(mapDish(resultSet));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to fetch dishes", exception);
        }
        return dishes;
    }

    @Override
    public Optional<Dish> findById(long id) {
        String query = "SELECT id, name, price, category FROM dishes WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDish(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load dish", exception);
        }
        return Optional.empty();
    }

    @Override
    public long save(Dish dish) {
        String sql = "INSERT INTO dishes(name, price, category) VALUES(?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dish.getName());
            statement.setDouble(2, dish.getPrice());
            statement.setString(3, dish.getCategory().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    dish.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save dish", exception);
        }
        return -1L;
    }

    @Override
    public void update(Dish dish) {
        String sql = "UPDATE dishes SET name = ?, price = ?, category = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dish.getName());
            statement.setDouble(2, dish.getPrice());
            statement.setString(3, dish.getCategory().name());
            statement.setLong(4, dish.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update dish", exception);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM dishes WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete dish", exception);
        }
    }

    private Dish mapDish(ResultSet resultSet) throws SQLException {
        Dish dish = new Dish();
        dish.setId(resultSet.getLong("id"));
        dish.setName(resultSet.getString("name"));
        dish.setPrice(resultSet.getDouble("price"));
        dish.setCategory(DishCategory.valueOf(resultSet.getString("category")));
        return dish;
    }
}

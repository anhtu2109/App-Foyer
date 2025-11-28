package backend.repository.impl;

import backend.config.DatabaseConfig;
import backend.entity.Dish;
import backend.entity.Order;
import backend.entity.OrderItem;
import backend.entity.DishCategory;
import backend.repository.OrderRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderRepositoryImpl implements OrderRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, customer_name, status, created_at FROM orders ORDER BY datetime(created_at) DESC";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Order order = mapOrder(resultSet);
                order.setItems(loadItemsForOrder(connection, order.getId()));
                orders.add(order);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to load orders", exception);
        }
        return orders;
    }

    @Override
    public Optional<Order> findById(long id) {
        String sql = "SELECT id, customer_name, status, created_at FROM orders WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Order order = mapOrder(resultSet);
                    order.setItems(loadItemsForOrder(connection, id));
                    return Optional.of(order);
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to fetch order", exception);
        }
        return Optional.empty();
    }

    @Override
    public long create(Order order) {
        String orderSql = "INSERT INTO orders(customer_name, status, created_at, total) VALUES(?, ?, ?, ?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, order.getCustomerName());
            statement.setString(2, order.getStatus());
            statement.setString(3, order.getCreatedAt().format(FORMATTER));
            statement.setDouble(4, order.getTotal());
            statement.executeUpdate();
            long orderId;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Order insert did not return id");
                }
                orderId = keys.getLong(1);
            }
            saveItems(connection, orderId, order.getItems());
            return orderId;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to create order", exception);
        }
    }

    @Override
    public void update(Order order) {
        String updateSql = "UPDATE orders SET customer_name = ?, status = ?, total = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setString(1, order.getCustomerName());
            statement.setString(2, order.getStatus());
            statement.setDouble(3, order.getTotal());
            statement.setLong(4, order.getId());
            statement.executeUpdate();
            deleteItems(connection, order.getId());
            saveItems(connection, order.getId(), order.getItems());
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update order", exception);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete order", exception);
        }
    }

    private void saveItems(Connection connection, long orderId, List<OrderItem> items) throws SQLException {
        String insertSql = "INSERT INTO order_items(order_id, dish_id, quantity, price) VALUES(?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (OrderItem item : items) {
                statement.setLong(1, orderId);
                statement.setLong(2, item.getDish().getId());
                statement.setInt(3, item.getQuantity());
                statement.setDouble(4, item.getPrice());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void deleteItems(Connection connection, long orderId) throws SQLException {
        String deleteSql = "DELETE FROM order_items WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    private List<OrderItem> loadItemsForOrder(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT oi.id, oi.quantity, oi.price, d.id as dishId, d.name, d.price as dishPrice, d.category as dishCategory " +
                "FROM order_items oi INNER JOIN dishes d ON oi.dish_id = d.id WHERE order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(resultSet.getLong("id"));
                    Dish dish = new Dish();
                    dish.setId(resultSet.getLong("dishId"));
                    dish.setName(resultSet.getString("name"));
                    dish.setPrice(resultSet.getDouble("dishPrice"));
                    dish.setCategory(DishCategory.valueOf(resultSet.getString("dishCategory")));
                    item.setDish(dish);
                    item.setQuantity(resultSet.getInt("quantity"));
                    item.setPrice(resultSet.getDouble("price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setId(resultSet.getLong("id"));
        order.setCustomerName(resultSet.getString("customer_name"));
        order.setStatus(resultSet.getString("status"));
        String createdAt = resultSet.getString("created_at");
        order.setCreatedAt(LocalDateTime.parse(createdAt, FORMATTER));
        return order;
    }
}

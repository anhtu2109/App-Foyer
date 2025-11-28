package backend.entity;

public class OrderItem {
    private Long id;
    private Dish dish;
    private int quantity;
    private double price;

    public OrderItem() {
    }

    public OrderItem(Long id, Dish dish, int quantity, double price) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
        this.price = price;
    }

    public OrderItem(Dish dish, int quantity) {
        this(null, dish, quantity, dish.getPrice());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getLineTotal() {
        return price * quantity;
    }
}

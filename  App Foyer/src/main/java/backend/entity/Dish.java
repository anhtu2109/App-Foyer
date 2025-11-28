package backend.entity;

public class Dish {
    private Long id;
    private String name;
    private double price;
    private DishCategory category;
    

    public Dish() {
    }

    public Dish(Long id, String name, double price, DishCategory category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Dish(String name, double price, DishCategory category) {
        this(null, name, price, category);
    }

    public Dish(String name, double price) {
        this(name, price, DishCategory.ENTREE);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public DishCategory getCategory() {
        return category;
    }

    public void setCategory(DishCategory category) {
        this.category = category;
    }
}

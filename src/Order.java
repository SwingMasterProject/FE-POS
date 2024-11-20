
public class Order {
    private final int tableNumber;
    private final String itemName;
    private int quantity;
    private final int price;

    public Order(int tableNumber, String itemName, int quantity, int price) {
        this.tableNumber = tableNumber;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }
}

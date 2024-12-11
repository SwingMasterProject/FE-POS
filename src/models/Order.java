package models;

public class Order {
    private int tableNumber;
    private String menuId;
    private String itemName;
    private int quantity;
    private int price;

    // 생성자 및 getter/setter 추가
    public Order(int tableNumber, String menuId, String itemName, int quantity, int price) {
        this.tableNumber = tableNumber;
        this.menuId = menuId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    // Getter
    public int getTableNumber() {
        return tableNumber;
    }

    public String getMenuId() {
        return menuId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

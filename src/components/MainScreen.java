package components;

import models.Order;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainScreen extends JPanel {
    private final List<Order> orders;

    public MainScreen(List<Order> orders) {
        this.orders = orders;
        setLayout(new BorderLayout());
        createMainScreen();
    }

    public void createMainScreen() {
        JPanel orderPanel = new JPanel();
        orderPanel.setLayout(new GridLayout(4, 5, 5, 5));
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 1; i <= 20; i++) {
            JButton tableButton = createTableButton(i);
            orderPanel.add(tableButton);
        }

        add(orderPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JButton createTableButton(int tableNumber) {
        JButton tableButton = new JButton();
        tableButton.setLayout(new BorderLayout());
        tableButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        tableButton.setBackground(Color.LIGHT_GRAY);

        JLabel tableLabel = new JLabel("Table " + tableNumber, SwingConstants.CENTER);
        tableLabel.setFont(new Font("Arial", Font.BOLD, 14));
        tableButton.add(tableLabel, BorderLayout.NORTH);

        JLabel orderSummaryLabel = new JLabel("<html><center>No Orders</center></html>", SwingConstants.CENTER);
        tableButton.add(orderSummaryLabel, BorderLayout.CENTER);

        updateButtonAppearance(tableButton, orderSummaryLabel, tableNumber);

        // ActionListener 추가
        tableButton.addActionListener(e -> {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            new TableDetailsScreen(tableNumber, orders, this); // 세부 내용 화면 호출
        });

        return tableButton;
    }

    private void updateButtonAppearance(JButton tableButton, JLabel orderSummaryLabel, int tableNumber) {
        boolean hasOrder = false;

        // 테이블에 해당하는 주문 데이터 확인
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                tableButton.setBackground(Color.PINK); // 주문이 있는 테이블은 핑크색으로 표시
                int total = orders.stream()
                        .filter(o -> o.getTableNumber() == tableNumber)
                        .mapToInt(o -> o.getPrice() * o.getQuantity())
                        .sum();
                orderSummaryLabel.setText(String.format(
                        "<html><center>%s 외 %d개<br>합계: %d원</center></html>",
                        order.getItemName(), // 첫 번째 주문의 메뉴명
                        (int) orders.stream().filter(o -> o.getTableNumber() == tableNumber).count() - 1, // 추가 주문 개수
                        total // 합계
                ));
                hasOrder = true;
                break;
            }
        }

        if (!hasOrder) {
            tableButton.setBackground(Color.LIGHT_GRAY); // 주문이 없는 경우 기본 색상
            orderSummaryLabel.setText("<html><center>No Orders</center></html>");
        }
    }

    public void updateTable(int tableNumber, List<Order> updatedOrders) {
        System.out.println("업데이트된 테이블 번호: " + tableNumber);
        System.out.println("업데이트된 주문 데이터: " + updatedOrders);
        orders.clear();
        orders.addAll(updatedOrders);
        removeAll(); // 기존 UI 제거
        createMainScreen(); // 새로운 UI 생성
        revalidate();
        repaint();
    }

}

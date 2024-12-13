package components;

import models.Order;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainScreen extends JPanel {
    private final List<Order> orders;
    private final List<Integer> reservedTables = new ArrayList<>();

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

        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                hasOrder = true;

                // 합계 금액 계산
                int total = orders.stream()
                        .filter(o -> o.getTableNumber() == tableNumber)
                        .mapToInt(o -> o.getPrice() * o.getQuantity())
                        .sum();

                // UI 업데이트
                String summaryText = String.format(
                        "<html><center>%s 외 %d개<br>합계: %d원</center></html>",
                        order.getItemName(),
                        (int) orders.stream().filter(o -> o.getTableNumber() == tableNumber).count() - 1,
                        total);

                orderSummaryLabel.setText(summaryText);
                tableButton.setBackground(Color.PINK); // 주문이 있는 테이블은 핑크색으로 표시

                return; // 주문이 있으면 더 이상 탐색하지 않음
            }
        }

        // 주문이 없는 경우 처리
        if (!hasOrder) {
            tableButton.setBackground(Color.LIGHT_GRAY); // 기본 색상
            orderSummaryLabel.setText("<html><center>No Orders</center></html>");
        }
    }

    public void reserveTable(int tableNumber) {
        if (!reservedTables.contains(tableNumber)) {
            reservedTables.add(tableNumber); // 예약된 테이블 추가
        }
        removeAll(); // 기존 UI 제거
        createMainScreen(); // 새로운 UI 생성
        revalidate();
        repaint(); // 변경된 UI 반영
    }

    public void updateSpecificTable(int tableNumber, List<Order> updatedOrders) {
        // 기존 테이블의 주문 데이터 갱신
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
        orders.addAll(updatedOrders);

        // 특정 테이블 버튼 UI 갱신
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                for (Component button : panel.getComponents()) {
                    if (button instanceof JButton) {
                        JButton tableButton = (JButton) button;

                        // 테이블 번호 확인
                        if (tableButton.getText() != null && tableButton.getText().contains("Table " + tableNumber)) {
                            JLabel orderSummaryLabel = (JLabel) tableButton.getComponent(1); // 버튼의 두 번째 컴포넌트 (라벨)
                            updateButtonAppearance(tableButton, orderSummaryLabel, tableNumber); // UI 업데이트
                            break;
                        }
                    }
                }
            }
        }

        // UI 강제 갱신
        revalidate();
        repaint();
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

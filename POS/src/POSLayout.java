import javax.swing.*;
import java.awt.*;

public class POSLayout extends JFrame {
    public POSLayout() {
        setTitle("POS System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // 주문 목록 패널 생성
        JPanel orderPanel = new JPanel();
        orderPanel.setLayout(new GridLayout(4, 5, 5, 5)); // 4x5 그리드 레이아웃
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 주문 목록 예시 항목들 추가
        for (int i = 0; i < 20; i++) {
            JButton itemPanel = new JButton();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel tableLabel = new JLabel("1 Table");
            JLabel itemLabel = new JLabel("김치찌개 (1)");
            JLabel quantityLabel = new JLabel("수량: 1");
            JLabel priceLabel = new JLabel("합계: 10,000");

            itemPanel.add(tableLabel, BorderLayout.NORTH);
            itemPanel.add(itemLabel, BorderLayout.CENTER);
            itemPanel.add(priceLabel, BorderLayout.SOUTH);

            // 색상 설정 (첫 번째 항목만 강조 색상)
            if (i == 0) {
                itemPanel.setBackground(Color.PINK);
                priceLabel.setText("합계: 36,000");
                priceLabel.setForeground(Color.RED);
            }

            orderPanel.add(itemPanel);
        }

        // 기능 버튼 패널 생성
        JPanel functionPanel = new JPanel();
        functionPanel.setLayout(new GridLayout(6 , 1, 5, 5)); // 1열 8행 그리드 레이아웃
        functionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 기능 버튼 예시 추가
        String[] functionNames = { "직원 관리", "메뉴 등록", "메뉴 삭제", "일일 인기 메뉴", "총 매출액", "요청 사항" };
        for (String name : functionNames) {
            JButton functionButton = new JButton(name);
            functionPanel.add(functionButton);
        }

        // 주요 패널 추가
        add(orderPanel, BorderLayout.CENTER);
        add(functionPanel, BorderLayout.EAST);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new POSLayout());
    }
}

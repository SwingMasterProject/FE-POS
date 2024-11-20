import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.*;

public class POSLayout extends JFrame {
    private final List<Order> orders; // 주문 데이터를 저장하는 리스트
    private final List<String> availableMenus; // 추가 가능한 메뉴 목록
    private JPanel orderPanel; // 메인 화면의 주문 목록 패널

    public POSLayout() {
        setTitle("POS System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // 주문 데이터 초기화
        orders = new ArrayList<>();
        availableMenus = new ArrayList<>();
        initializeOrders();
        initializeMenus();

        // 메인 화면 구성
        createMainScreen();

        // 기능 버튼 패널
        JPanel functionPanel = new JPanel();
        functionPanel.setLayout(new GridLayout(6, 1, 5, 5));
        functionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] functionNames = { "직원 관리", "메뉴 등록", "메뉴 편집", "일일 인기 메뉴", "총 매출액", "요청 사항" };
        for (String name : functionNames) {
            JButton functionButton = new JButton(name);
            functionPanel.add(functionButton);
        }

        add(orderPanel, BorderLayout.CENTER);
        add(functionPanel, BorderLayout.EAST);

        setVisible(true);
    }

    /**
     * 메인 화면 구성
     */
    private void createMainScreen() {
        orderPanel = new JPanel();
        orderPanel.setLayout(new GridLayout(4, 5, 5, 5)); // 4x5 그리드 레이아웃
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 주문 목록 항목 동적 생성
        for (int i = 1; i <= 20; i++) {
            JButton itemPanel = new JButton();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel tableLabel = new JLabel("Table " + i);
            JLabel orderSummaryLabel = new JLabel("<html><center>No Orders</center></html>");
            orderSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

            itemPanel.add(tableLabel, BorderLayout.NORTH);
            itemPanel.add(orderSummaryLabel, BorderLayout.CENTER);

            // 주문이 있는 테이블 강조 및 요약 표시
            boolean hasOrders = false;
            int tableTotal = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == i) {
                    hasOrders = true;
                    itemPanel.setBackground(Color.PINK);

                    tableTotal += order.getQuantity() * order.getPrice();
                    orderSummaryLabel.setText(
                            String.format(
                                    "<html><center>%s 외 %d개<br>합계: <span style='color:red;'>%d원</span></center></html>",
                                    order.getItemName(),
                                    getOrderCountForTable(i) - 1, // 남은 메뉴 수 표시
                                    tableTotal));

                    // 클릭 시 세부 정보 표시
                    int tableNumber = order.getTableNumber();
                    itemPanel.addActionListener(e -> showOrderDetails(tableNumber));
                }
            }

            // 주문이 없는 경우 기본 색상 유지
            if (!hasOrders) {
                itemPanel.setBackground(Color.LIGHT_GRAY);
            }

            orderPanel.add(itemPanel);
        }
    }

    /**
     * 테이블에 포함된 주문 수를 계산
     */
    private int getOrderCountForTable(int tableNumber) {
        int count = 0;
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                count++;
            }
        }
        return count;
    }

    /**
     * 세부 주문 정보 창 표시
     */
    private void showOrderDetails(int tableNumber) {
        JFrame detailsFrame = new JFrame("테이블 세부 내용 - Table " + tableNumber);
        detailsFrame.setSize(800, 400); // 오른쪽 메뉴 추가를 위해 너비 확장
        detailsFrame.setLayout(new BorderLayout(10, 10));
        // 상단: 테이블 정보
        JPanel tableInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableInfoPanel.add(new JLabel("Table: " + tableNumber));

        // 중앙: 주문 목록
        JPanel orderListPanel = new JPanel();
        orderListPanel.setLayout(new GridLayout(0, 3, 10, 10)); // 주문 정보는 여전히 GridLayout 사용
        orderListPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));

        int totalAmount = 0; // 총합 계산
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                orderListPanel.add(new JLabel(order.getItemName()));
                orderListPanel.add(new JLabel("(" + order.getQuantity() + ")"));
                orderListPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원"));
                totalAmount += order.getPrice() * order.getQuantity();
            }
        }

        // 오른쪽: 메뉴 추가 버튼들
        JPanel menuPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        menuPanel.setBorder(BorderFactory.createTitledBorder("추가할 메뉴"));
        for (String menu : availableMenus) {
            JButton menuButton = new JButton(menu);
            menuButton.addActionListener(e -> {
                // 선택된 메뉴를 테이블 주문에 추가
                addMenuToTable(tableNumber, menu, 10000); // 메뉴 추가 (가격은 임시로 10,000원)
                detailsFrame.dispose(); // 창 닫기
                showOrderDetails(tableNumber); // 새로고침
            });
            menuPanel.add(menuButton);
        }

        // 하단: 합계 및 버튼
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout()); // 하단 영역도 BorderLayout 사용
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 합계 표시 (빨간색 강조)
        JLabel totalLabel = new JLabel("<html>합계: <span style='color:red;'>" + totalAmount + "원</span></html>");
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16)); // 굵은 글씨체 적용
        bottomPanel.add(totalLabel, BorderLayout.NORTH);

        // 주요 버튼들
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // 버튼 중앙 정렬
        buttonPanel.add(new JButton("전체 취소"));
        buttonPanel.add(new JButton("+"));
        buttonPanel.add(new JButton("-"));

        // 주문하기 버튼: 창 닫기 및 메인 화면 갱신 //FIXME: 메뉴 갱신 안되는 부분 수정해야함
        JButton submitButton = new JButton("주문하기");
        submitButton.addActionListener(e -> {
            updateMainScreen(); // 메인 화면 갱신
            detailsFrame.dispose(); // 세부 화면 닫기
        });
        buttonPanel.add(submitButton);

        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        // 추가 버튼들 (예약, 영수증)
        JPanel extraButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        extraButtonPanel.add(new JButton("예약"));
        extraButtonPanel.add(new JButton("영수증"));
        bottomPanel.add(extraButtonPanel, BorderLayout.SOUTH);

        // BorderLayout에 각 영역 추가
        detailsFrame.add(tableInfoPanel, BorderLayout.NORTH); // 상단 테이블 정보
        detailsFrame.add(orderListPanel, BorderLayout.CENTER); // 중앙 주문 목록
        detailsFrame.add(menuPanel, BorderLayout.EAST); // 오른쪽 메뉴 추가 버튼
        detailsFrame.add(bottomPanel, BorderLayout.SOUTH); // 하단 버튼과 합계

        detailsFrame.setVisible(true);
    }

    /**
     * 메뉴를 테이블에 추가 //FIXME: 메뉴 갱신 안되는 부분 수정해야함
     */
    private void addMenuToTable(int tableNumber, String menuName, int price) {
        boolean menuExists = false;
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber && order.getItemName().equals(menuName)) {
                order.setQuantity(order.getQuantity() + 1); // 메뉴 수량 증가
                menuExists = true;
                break;
            }
        }
        if (!menuExists) {
            orders.add(new Order(tableNumber, menuName, 1, price)); // 새 메뉴 추가
        }
    }

    /**
     * 메인 화면 갱신
     */
    private void updateMainScreen() {
        orderPanel.removeAll(); // 기존 패널 내용 제거
        createMainScreen(); // 메인 화면 재구성
        orderPanel.revalidate(); // 화면 갱신
        orderPanel.repaint(); // 화면 다시 그리기
    }

    /**
     * 초기 주문 데이터 설정
     */
    private void initializeOrders() {
        orders.add(new Order(1, "김치찌개", 2, 12000));
        orders.add(new Order(1, "삼겹살", 2, 13000));
        orders.add(new Order(3, "된장찌개", 1, 8000));
        orders.add(new Order(5, "비빔밥", 1, 10000));
    }

    /**
     * 초기 메뉴 데이터 설정
     */
    private void initializeMenus() {
        availableMenus.add("김치찌개");
        availableMenus.add("된장찌개");
        availableMenus.add("비빔밥");
        availableMenus.add("불고기");
        availableMenus.add("삼겹살");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new POSLayout());
    }
}

/**
 * 주문 데이터를 나타내는 클래스
 */
class Order {
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
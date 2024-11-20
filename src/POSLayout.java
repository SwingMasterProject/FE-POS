import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

public class POSLayout extends JFrame {
    private final List<Order> orders; // 주문 데이터를 저장하는 리스트
    private final List<String> availableMenus; // 추가 가능한 메뉴 목록
    private JPanel orderPanel; // 메인 화면의 주문 목록 패널
    private final List<Integer> reservedTables; // 예약된 테이블 번호 리스트

    public POSLayout() {
        setTitle("POS System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // 주문 데이터 초기화
        orders = new ArrayList<>();
        availableMenus = new ArrayList<>();
        reservedTables = new ArrayList<>();
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
            functionButton.addActionListener(e -> {
                if (name.equals("메뉴 등록")) {
                    addMenu();
                } else if (name.equals("메뉴 편집")) {
                    editMenu();
                }
            });
            functionPanel.add(functionButton);
        }

        add(orderPanel, BorderLayout.CENTER);
        add(functionPanel, BorderLayout.EAST);

        setVisible(true);
    }

    private void createMainScreen() {
        if (orderPanel != null) {
            remove(orderPanel); // 기존 패널 제거
        }

        orderPanel = new JPanel();
        orderPanel.setLayout(new GridLayout(4, 5, 5, 5)); // 4x5 그리드 레이아웃
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 1; i <= 20; i++) {
            JButton itemPanel = new JButton();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel tableLabel = new JLabel("Table " + i);
            JLabel orderSummaryLabel = new JLabel("<html><center>No Orders</center></html>");
            orderSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

            itemPanel.add(tableLabel, BorderLayout.NORTH);
            itemPanel.add(orderSummaryLabel, BorderLayout.CENTER);

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
                                    getOrderCountForTable(i) - 1,
                                    tableTotal));
                }
            }

            // 예약된 테이블은 파란색으로 표시
            if (reservedTables.contains(i)) {
                itemPanel.setBackground(Color.cyan);
            } else if (!hasOrders) {
                itemPanel.setBackground(Color.LIGHT_GRAY);
            }

            int tableNumber = i;
            itemPanel.addActionListener(e -> showOrderDetails(tableNumber));

            orderPanel.add(itemPanel);
        }

        add(orderPanel, BorderLayout.CENTER);
        revalidate(); // 레이아웃 갱신
        repaint(); // 화면 다시 그리기
    }

    private int getOrderCountForTable(int tableNumber) {
        return (int) orders.stream().filter(order -> order.getTableNumber() == tableNumber).count();
    }

    private void showOrderDetails(int tableNumber) {
        JFrame detailsFrame = new JFrame("테이블 세부 내용 - Table " + tableNumber);
        detailsFrame.setSize(800, 400);
        detailsFrame.setLayout(new BorderLayout(10, 10));

        JPanel tableInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableInfoPanel.add(new JLabel("Table: " + tableNumber));

        JPanel orderListPanel = new JPanel();
        orderListPanel.setLayout(new GridLayout(0, 3, 10, 10));
        orderListPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));

        int totalAmount = 0;
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                orderListPanel.add(new JLabel(order.getItemName()));
                orderListPanel.add(new JLabel("(" + order.getQuantity() + ")"));
                orderListPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원"));
                totalAmount += order.getPrice() * order.getQuantity();
            }
        }

        JPanel menuPanel = new JPanel(new GridLayout(4, 4, 5, 5));
        menuPanel.setBorder(BorderFactory.createTitledBorder("추가할 메뉴"));
        for (String menu : availableMenus) {
            JButton menuButton = new JButton(menu);
            menuButton.addActionListener(e -> {
                addMenuToTable(tableNumber, menu, 10000);
                updateMainScreen();
                detailsFrame.dispose();
                showOrderDetails(tableNumber);
            });
            menuPanel.add(menuButton);
        }

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel totalLabel = new JLabel("<html>합계: <span style='color:red;'>" + totalAmount + "원</span></html>");
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bottomPanel.add(totalLabel, BorderLayout.NORTH);

        // 기존 버튼들과 영수증, 예약 버튼 추가
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("전체 취소");
        cancelButton.addActionListener(e -> {
            clearOrdersForTable(tableNumber);
            updateMainScreen();
            detailsFrame.dispose();
        });
        buttonPanel.add(cancelButton);

        JButton plusButton = new JButton("+");
        JButton minusButton = new JButton("-");
        buttonPanel.add(plusButton);
        buttonPanel.add(minusButton);

        JButton orderButton = new JButton("주문하기");
        orderButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(detailsFrame, "주문이 완료되었습니다!");
            detailsFrame.dispose();
        });
        buttonPanel.add(orderButton);

        JButton receiptButton = new JButton("영수증");
        receiptButton.addActionListener(e -> {
            generateReceipt(tableNumber);
            clearOrdersForTable(tableNumber);
            updateMainScreen();
            detailsFrame.dispose();
        });
        buttonPanel.add(receiptButton);

        JButton reservationButton = new JButton("예약");
        reservationButton.addActionListener(e -> {
            reserveTable(tableNumber); // 테이블 예약 처리
            updateMainScreen();
            detailsFrame.dispose();
        });
        buttonPanel.add(reservationButton);

        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        detailsFrame.add(tableInfoPanel, BorderLayout.NORTH);
        detailsFrame.add(orderListPanel, BorderLayout.CENTER);
        detailsFrame.add(menuPanel, BorderLayout.EAST);
        detailsFrame.add(bottomPanel, BorderLayout.SOUTH);

        detailsFrame.setVisible(true);
    }

    private void reserveTable(int tableNumber) {
        if (!reservedTables.contains(tableNumber)) {
            reservedTables.add(tableNumber);
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "이(가) 예약되었습니다!");
        } else {
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "은(는) 이미 예약되었습니다.");
        }
    }

    private void addMenuToTable(int tableNumber, String menuName, int price) {
        orders.stream()
                .filter(order -> order.getTableNumber() == tableNumber && order.getItemName().equals(menuName))
                .findFirst()
                .ifPresentOrElse(
                        order -> order.setQuantity(order.getQuantity() + 1),
                        () -> orders.add(new Order(tableNumber, menuName, 1, price)));
    }

    private void updateMainScreen() {
        createMainScreen();
    }

    private void generateReceipt(int tableNumber) {
        try {
            String fileName = "Receipt_Table_" + tableNumber + ".pdf";
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("영수증"));
            document.add(new Paragraph("Table: " + tableNumber));
            document.add(new Paragraph("----------------------"));

            int totalAmount = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber) {
                    document.add(new Paragraph(
                            String.format("%s x %d = %d원", order.getItemName(), order.getQuantity(),
                                    order.getQuantity() * order.getPrice())));
                    totalAmount += order.getQuantity() * order.getPrice();
                }
            }

            document.add(new Paragraph("----------------------"));
            document.add(new Paragraph("총 합계: " + totalAmount + "원"));

            document.close();
            JOptionPane.showMessageDialog(this, "영수증이 저장되었습니다: " + fileName);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "영수증 생성 중 오류가 발생했습니다.");
        }
    }

    private void clearOrdersForTable(int tableNumber) {
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
    }

    private void addMenu() {
        String newMenu = JOptionPane.showInputDialog(this, "새로운 메뉴 이름을 입력하세요:");
        if (newMenu != null && !newMenu.trim().isEmpty()) {
            availableMenus.add(newMenu);
        }
    }

    private void editMenu() {
        String menuToEdit = (String) JOptionPane.showInputDialog(this, "수정할 메뉴를 선택하세요:", "메뉴 수정",
                JOptionPane.PLAIN_MESSAGE, null, availableMenus.toArray(), null);
        if (menuToEdit != null) {
            String newName = JOptionPane.showInputDialog(this, "새로운 메뉴 이름을 입력하세요:", menuToEdit);
            if (newName != null && !newName.trim().isEmpty()) {
                availableMenus.set(availableMenus.indexOf(menuToEdit), newName);
            }
        }
    }

    private void initializeOrders() {
        orders.add(new Order(1, "김치찌개", 2, 12000));
        orders.add(new Order(1, "삼겹살", 2, 13000));
        orders.add(new Order(3, "된장찌개", 1, 8000));
        orders.add(new Order(5, "비빔밥", 1, 10000));
    }

    private void initializeMenus() {
        availableMenus.add("김치찌개");
        availableMenus.add("된장찌개");
        availableMenus.add("비빔밥");
        availableMenus.add("불고기");
        availableMenus.add("삼겹살");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(POSLayout::new);
    }
}

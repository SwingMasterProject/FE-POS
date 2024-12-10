import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.*;
import java.io.IOException;

public class POS2 extends JFrame {
    private final Gson gson = new Gson(); // Gson 객체 생성
    private final OkHttpClient httpClient = new OkHttpClient();
    private final List<Order> orders; // 주문 데이터를 저장하는 리스트
    private final List<String> availableMenus; // 추가 가능한 메뉴 목록
    private JPanel orderPanel; // 메인 화면의 주문 목록 패널
    private final List<Integer> reservedTables; // 예약된 테이블 번호 리스트
    private final Map<String, Integer> menuWithPrices = new HashMap<>();
    private final Map<String, List<MenuItem>> categorizedMenus = new HashMap<>();
    private final List<Employee> employees = new ArrayList<>();
    private int addPrice;
    private final String BASE_URL = "https://be-api-seven.vercel.app/";

    private void fetchMenus() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "api/menu")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "메뉴 가져오기 실패: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("Response: " + responseBody); // 디버깅용 출력

                    try {
                        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray menuItemsArray = jsonObject.getAsJsonArray("menuItems");
                        MenuItem[] menuItems = gson.fromJson(menuItemsArray, MenuItem[].class);

                        SwingUtilities.invokeLater(() -> {
                            availableMenus.clear();
                            categorizedMenus.clear(); // 카테고리별 메뉴 초기화
                            menuWithPrices.clear();

                            for (MenuItem menuItem : menuItems) {
                                availableMenus.add(menuItem.getName());
                                menuWithPrices.put(menuItem.getName(), menuItem.getPrice());

                                // 카테고리에 따라 메뉴 추가
                                categorizedMenus
                                        .computeIfAbsent(menuItem.getCategory(), k -> new ArrayList<>())
                                        .add(menuItem);
                            }

                            createMainScreen(); // 화면 갱신
                            JOptionPane.showMessageDialog(null, "메뉴가 성공적으로 업데이트되었습니다!");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        SwingUtilities
                                .invokeLater(() -> JOptionPane.showMessageDialog(null, "응답 파싱 실패: " + e.getMessage()));
                    }
                } else {
                    SwingUtilities.invokeLater(
                            () -> JOptionPane.showMessageDialog(null, "메뉴 가져오기 실패: " + response.message()));
                }
            }
        });
    }

    public POSLayout() {
        setTitle("POS System"); // 전체화면 title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600); // 전체화면 사이즈
        setLayout(new BorderLayout()); // 전체화면 layout

        // 주문 데이터 초기화
        orders = new ArrayList<>();
        availableMenus = new ArrayList<>();
        reservedTables = new ArrayList<>();
        initializeOrders();
        fetchMenus();

        // 메인 화면 구성
        createMainScreen();

        // 기능 버튼 패널
        JPanel functionPanel = new JPanel();
        functionPanel.setLayout(new GridLayout(6, 1, 5, 5));
        functionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] functionNames = { "직원 관리", "메뉴 등록", "메뉴 편집", "일일 인기 메뉴", "총 매출액", "요청 사항" }; // 전체화면 버튼
        for (String name : functionNames) {
            JButton functionButton = new JButton(name);
            functionButton.addActionListener(e -> {
                if (name.equals("메뉴 등록")) {
                    addMenu();
                } else if (name.equals("메뉴 편집")) {
                    editMenu();
                }
                if (name.equals("직원 관리")) {
                    manageEmployees();
                }
                // 인기메뉴 버튼 코드
                if (name.equals("일일 인기 메뉴")) {
                    Map<String, Integer> menuCount = new HashMap<>();
                    for (Order order : orders) {
                        menuCount.put(order.getItemName(),
                                menuCount.getOrDefault(order.getItemName(), 0) + order.getQuantity());
                    }
                    String popularMenu = menuCount.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("No Orders");
                    JOptionPane.showMessageDialog(this, "오늘의 인기 메뉴: " + popularMenu);
                }
                // 일일 총매출액 버튼 코드
                if (name.equals("총 매출액")) {
                    int totalSales = orders.stream()
                            .mapToInt(order -> order.getQuantity() * order.getPrice())
                            .sum();
                    JOptionPane.showMessageDialog(this, "총 매출액: " + totalSales + "원");
                }
                if (name.equals("요청 사항")) {
                    JOptionPane.showMessageDialog(this, "요청 사항 기능은 현재 준비 중입니다!");
                }
            });
            functionPanel.add(functionButton);
        }

        add(orderPanel, BorderLayout.CENTER);
        add(functionPanel, BorderLayout.EAST);

        setVisible(true);
    }

    private void manageEmployees() {
        String[] options = { "출근", "퇴근", "출퇴근 기록 보기" };
        String choice = (String) JOptionPane.showInputDialog(this, "직원 관리", "직원관리", JOptionPane.PLAIN_MESSAGE, null,
                options, options[0]);
        if ("출근".equals(choice)) {
            checkInEmployee();
        } else if ("퇴근".equals(choice)) {
            checkOutEmployee();
        } else if ("출퇴근 기록 보기".equals(choice)) {
            viewEmployeeRecords();
        }
    }

    // TODO: 유저 id 값 줘야함
    private void checkInEmployee() {
        String name = JOptionPane.showInputDialog(this, "이름을 입력하시오: ");
        if (name != null && !name.trim().isEmpty()) {
            Employee employee = new Employee(name, LocalDateTime.now(), null);
            employees.add(employee);
            sendEmployeeDataToServer(employee, "api/time-records/clock-in");
            JOptionPane.showMessageDialog(this, name + " 출근 등록 완료");
        } else {
            JOptionPane.showMessageDialog(this, "이름을 입력하시오: ");
        }
    }

    private void checkOutEmployee() {
        String name = JOptionPane.showInputDialog(this, "퇴근자의 이름을 입력하시오: ");
        if (name != null && !name.trim().isEmpty()) {
            Employee employee = employees.stream()
                    .filter(e -> e.getName().equals(name) && e.getCheckOutTime() == null)
                    .findFirst()
                    .orElse(null);

            if (employee != null) {
                employee.setCheckOutTime(LocalDateTime.now());
                sendEmployeeDataToServer(employee, "api/time-records/clock-out");
                JOptionPane.showMessageDialog(this, name + " 퇴근 등록 완료");
            } else {
                JOptionPane.showMessageDialog(this, "출근 기록이 없거나 이미 퇴근했습니다.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.");
        }
    }

    private void viewEmployeeRecords() {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "api/time-records") // API 엔드포인트
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    JOptionPane.showMessageDialog(null, "직원 데이터를 불러오는데 실패했습니다: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Employee[] employeesFromServer = gson.fromJson(responseBody, Employee[].class);

                        SwingUtilities.invokeLater(() -> {
                            StringBuilder records = new StringBuilder("<html><body>");
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                            for (Employee employee : employeesFromServer) {
                                records.append(employee.getName())
                                        .append(" - 출근: ")
                                        .append(employee.getCheckInTime().format(formatter));
                                if (employee.getCheckOutTime() != null) {
                                    records.append(", 퇴근: ").append(employee.getCheckOutTime().format(formatter));
                                } else {
                                    records.append(", 퇴근: 기록 없음");
                                }
                                records.append("<br>");
                            }
                            records.append("</body></html>");

                            JOptionPane.showMessageDialog(null, new JLabel(records.toString()), "직원 출근/퇴근 기록",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    } else {
                        JOptionPane.showMessageDialog(null, "직원 데이터를 불러오는데 실패했습니다: " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "직원 데이터를 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    private void sendEmployeeDataToServer(Employee employee, String action) {
        try {
            String jsonData = gson.toJson(employee);

            RequestBody body = RequestBody.create(jsonData, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/time-records/" + action) // API 엔드포인트
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    JOptionPane.showMessageDialog(null, "데이터 전송 실패: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "데이터 전송 성공!"));
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                "데이터 전송 실패: " + response.message()));
                    }
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "데이터 전송 실패: " + e.getMessage());
        }
    }

    // 전체화면 코드
    private void createMainScreen() {
        if (orderPanel != null) {
            remove(orderPanel); // 기존 패널 제거
        }

        orderPanel = new JPanel();
        orderPanel.setLayout(new GridLayout(4, 5, 5, 5)); // 20개 좌석 표시
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 테이블 표시
        for (int i = 1; i <= 20; i++) {
            JButton itemPanel = new JButton();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 주문 없을떄 회색

            JLabel tableLabel = new JLabel("Table " + i); // 테이블 번호
            JLabel orderSummaryLabel = new JLabel("<html><center>No Orders</center></html>");
            orderSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

            itemPanel.add(tableLabel, BorderLayout.NORTH);
            itemPanel.add(orderSummaryLabel, BorderLayout.CENTER);

            // 테이블 정보 업데이트
            boolean hasOrders = false;
            int tableTotal = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == i) {
                    hasOrders = true; // 주문 = true
                    itemPanel.setBackground(Color.PINK); // 주문 있을떄 핑크로 표시

                    tableTotal += order.getQuantity() * order.getPrice(); // 총 금액 계산 코드
                    orderSummaryLabel.setText(
                            // 총 금액표시
                            String.format(
                                    "<html><center>%s 외 %d개<br>합계: <span style='color:red;'>%d원</span></center></html>",
                                    order.getItemName(),
                                    getOrderCountForTable(i) - 1,
                                    tableTotal));
                }
            }

            // 예약된 테이블 색상 변경
            if (reservedTables.contains(i)) {
                itemPanel.setBackground(Color.cyan); // 예약 시 파란색으로 표시
            } else if (!hasOrders) {
                itemPanel.setBackground(Color.LIGHT_GRAY); // 없을땐 그레이
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

    private List<String> getMenusForTable(int tableNumber) {
        List<String> menus = new ArrayList<>();
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                menus.add(order.getItemName());
            }
        }
        return menus;
    }

    // 테이블 세부 내용 확인 화면
    private void showOrderDetails(int tableNumber) {
        fetchMenus();
        JFrame detailsFrame = new JFrame("테이블 세부 내용 - Table " + tableNumber); // 프레임 title
        detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        detailsFrame.setSize(800, 400); // 프레임 사이즈
        detailsFrame.setLayout(new BorderLayout(10, 10));
        JPanel tableInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // 테이블 세부 주문 패널 위치
        tableInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableInfoPanel.add(new JLabel("Table: " + tableNumber)); // 테이블 번호

        JPanel orderListPanel = new JPanel(); // 주문목록 패널
        orderListPanel.setLayout(new GridLayout(0, 3, 10, 10));
        orderListPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));

        int totalAmount = 0; // 총 금액 코드
        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                orderListPanel.add(new JLabel(order.getItemName()));
                orderListPanel.add(new JLabel("(" + order.getQuantity() + ")")); // 주문 수량
                orderListPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원")); // 총 금액 계산 코드
                totalAmount += order.getPrice() * order.getQuantity();
            }
        }

        // 메뉴 리스트
        JPanel menuPanel = new JPanel(new GridLayout(4, 4, 5, 5)); // 버튼 나열(그리드 방식 사용)
        menuPanel.setBorder(BorderFactory.createTitledBorder("추가할 메뉴"));
        for (String menu : availableMenus) {
            int price = menuWithPrices.getOrDefault(menu, 0); // 가격 가져오기
            JButton menuButton = new JButton("<html>" + menu + "<br>" + price + "원</html>"); // 메뉴명과 가격 표시
            menuButton.addActionListener(e -> {
                addMenuToTable(tableNumber, menu, addPrice); // 테이블 번호, 메뉴명, 가격
                updateMainScreen(); // 추가한 메뉴 전체 화면에 적용
                detailsFrame.dispose(); // 프레임 닫기
                showOrderDetails(tableNumber); // 테이블 번호
            });
            menuPanel.add(menuButton);
        }

        JPanel bottomPanel = new JPanel(new BorderLayout()); // 아래버튼 구역
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // 메뉴가격 합계 표시
        JLabel totalLabel = new JLabel("<html>합계: <span style='color:red;'>" + totalAmount + "원</span></html>"); // 합계
        totalLabel.setHorizontalAlignment(SwingConstants.LEFT); // 합계 위치
        bottomPanel.add(totalLabel, BorderLayout.NORTH); // 합계 위치

        // 기존 버튼들과 영수증, 예약 버튼 추가
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("전체 취소"); // 전체취소 버튼
        cancelButton.addActionListener(e -> { // 전체취소 코드
            clearOrdersForTable(tableNumber); // 테이블 번호의 테이블 주문 지우기
            updateMainScreen(); // 전체 화면에 적용
            detailsFrame.dispose();
        });
        buttonPanel.add(cancelButton);

        JButton plusButton = new JButton("+"); // 수량증가 버튼
        // 메뉴 수량증가 다이어로그
        plusButton.addActionListener(e -> {
            String menuToIncrease = (String) JOptionPane.showInputDialog(detailsFrame, "수량을 증가시킬 메뉴를 선택하세요:",
                    "수량 증가", JOptionPane.PLAIN_MESSAGE, null, getMenusForTable(tableNumber).toArray(), null);
            if (menuToIncrease != null) {
                orders.stream()
                        .filter(order -> order.getTableNumber() == tableNumber
                                && order.getItemName().equals(menuToIncrease))
                        .findFirst()
                        .ifPresent(order -> {
                            order.setQuantity(order.getQuantity() + 1);
                            updateMainScreen();
                            detailsFrame.dispose();
                            showOrderDetails(tableNumber);
                        });
            }
        });

        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> {
            String menuToDecrease = (String) JOptionPane.showInputDialog(detailsFrame, "수량을 감소시킬 메뉴를 선택하세요:",
                    "수량 감소", JOptionPane.PLAIN_MESSAGE, null, getMenusForTable(tableNumber).toArray(), null);
            if (menuToDecrease != null) {
                orders.stream()
                        .filter(order -> order.getTableNumber() == tableNumber
                                && order.getItemName().equals(menuToDecrease))
                        .findFirst()
                        .ifPresent(order -> {
                            if (order.getQuantity() > 1) {
                                order.setQuantity(order.getQuantity() - 1);
                            } else {
                                orders.remove(order); // 수량이 0이면 주문 삭제
                            }
                            updateMainScreen();
                            detailsFrame.dispose();
                            showOrderDetails(tableNumber);
                        });
            }
        });

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

        JButton cancelReservationButton = new JButton("예약 취소");
        cancelReservationButton.addActionListener(e -> {
            if (reservedTables.contains(tableNumber)) {
                reservedTables.remove((Integer) tableNumber);
                JOptionPane.showMessageDialog(this, "Table " + tableNumber + "의 예약이 취소되었습니다!");
                updateMainScreen();
                detailsFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Table " + tableNumber + "은(는) 예약되지 않았습니다.");
            }
        });
        buttonPanel.add(cancelReservationButton);

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

    private void addMenuToTable(int tableNumber, String menuName, int defaultPrice) {
        int price = menuWithPrices.getOrDefault(menuName, defaultPrice); // 메뉴 가격 가져오기
        orders.stream()
                .filter(order -> order.getTableNumber() == tableNumber && order.getItemName().equals(menuName))
                .findFirst()
                .ifPresentOrElse(
                        order -> order.setQuantity(order.getQuantity() + 1),
                        () -> orders.add(new Order(tableNumber, menuName, 1, price))); // 올바른 가격 설정
    }

    private void updateMainScreen() {
        createMainScreen();
    }

    private void generateReceipt(int tableNumber) {
        try {
            // PDF 파일 이름 설정
            String fileName = "Receipt_Table_" + tableNumber + ".pdf";

            // PdfWriter와 PdfDocument 생성
            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);

            // Document 생성
            Document document = new Document(pdf);

            // PDF 내용 작성
            document.add(new Paragraph("영수증").setBold().setFontSize(16));
            document.add(new Paragraph("Table: " + tableNumber));
            document.add(new Paragraph("----------------------"));

            int totalAmount = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber) {
                    // 주문 항목 추가 (메뉴명, 수량, 가격을 모두 표시)
                    String orderDetails = String.format(
                            "%s x %d = %d원", // 메뉴명 x 수량 = 총 가격
                            order.getItemName(),
                            order.getQuantity(),
                            order.getQuantity() * order.getPrice());
                    document.add(new Paragraph(orderDetails)); // PDF에 추가
                    totalAmount += order.getQuantity() * order.getPrice();
                }
            }

            // 총 합계 추가
            document.add(new Paragraph("----------------------"));
            document.add(new Paragraph("총 합계: " + totalAmount + "원").setBold());

            // Document 닫기
            document.close();

            // PDF 생성 성공 메시지
            JOptionPane.showMessageDialog(this, "영수증이 저장되었습니다: " + fileName);
        } catch (Exception e) {
            // 오류 메시지 표시
            JOptionPane.showMessageDialog(this, "영수증 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void clearOrdersForTable(int tableNumber) {
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
    }

    private void addMenu() {
        // 새로운 메뉴 이름 입력받기
        String newMenu = JOptionPane.showInputDialog(this, "새로운 메뉴 이름을 입력하세요:");
        if (newMenu != null && !newMenu.trim().isEmpty()) {
            String newPrice = JOptionPane.showInputDialog(this, "메뉴 가격을 입력하세요:");
            if (newPrice != null && !newPrice.trim().isEmpty()) {
                try {
                    int price = Integer.parseInt(newPrice); // 가격을 정수로 변환
                    availableMenus.add(newMenu);
                    menuWithPrices.put(newMenu, price); // 메뉴와 가격 저장
                    JOptionPane.showMessageDialog(this, "메뉴가 추가되었습니다: " + newMenu + " (" + price + "원)");
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "유효한 숫자를 입력하세요."); // 숫자가 아닌 경우 처리
                }
            } else {
                JOptionPane.showMessageDialog(this, "가격을 입력하세요.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "메뉴 이름을 입력하세요.");
        }
    }

    private void editMenu() {
        String menuToEdit = (String) JOptionPane.showInputDialog(this, "수정할 메뉴를 선택하세요:", "메뉴 수정",
                JOptionPane.PLAIN_MESSAGE, null, availableMenus.toArray(), null);
        if (menuToEdit != null) {
            String newName = JOptionPane.showInputDialog(this, "새로운 메뉴 이름을 입력하세요:", menuToEdit);
            if (newName != null && !newName.trim().isEmpty()) {
                availableMenus.set(availableMenus.indexOf(menuToEdit), newName);

                // 가격 수정 추가
                String newPrice = JOptionPane.showInputDialog(this, "새로운 메뉴 가격을 입력하세요:",
                        menuWithPrices.get(menuToEdit));
                if (newPrice != null && !newPrice.trim().isEmpty()) {
                    try {
                        int price = Integer.parseInt(newPrice);
                        menuWithPrices.put(newName, price); // 메뉴 이름 변경에 따른 가격 업데이트
                        JOptionPane.showMessageDialog(this, "메뉴가 수정되었습니다: " + newName + " (" + price + "원)");
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "유효한 숫자를 입력하세요.");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "메뉴 이름을 입력하세요.");
            }
        }
    }

    private void initializeOrders() {
        orders.add(new Order(1, "김치찌개", 2, 12000));
        orders.add(new Order(1, "삼겹살", 2, 13000));
        orders.add(new Order(3, "된장찌개", 1, 8000));
        orders.add(new Order(5, "비빔밥", 1, 10000));
    }

    static class MenuItem {
        private String _id;
        private String name;
        private int price;
        private String imageUrl;
        private boolean available;
        private String category;

        public String getName() {
            return name;
        }

        public int getPrice() {
            return price;
        }

        public String getCategory() {
            return category;
        }
    }

    static class Employee {
        private final String name;
        private final LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;

        public Employee(String name, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
            this.name = name;
            this.checkInTime = checkInTime;
            this.checkOutTime = checkOutTime;
        }

        public String getName() {
            return name;
        }

        public LocalDateTime getCheckInTime() {
            return checkInTime;
        }

        public LocalDateTime getCheckOutTime() {
            return checkOutTime;
        }

        public void setCheckOutTime(LocalDateTime checkOutTime) {
            this.checkOutTime = checkOutTime;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(POSLayout::new);
    }
}

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

    public int getPrice() {
        return price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

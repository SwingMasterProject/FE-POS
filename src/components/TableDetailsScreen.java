package components;

import models.Order;
import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDetailsScreen extends JFrame {
    private final MainScreen mainScreen;
    private final List<Order> orders;
    private final List<Integer> reservedTables = new ArrayList<>();
    private final List<String> availableMenus = new ArrayList<>();
    private final Map<String, Integer> menuWithPrices = new HashMap<>();
    private final Map<String, String> menuWithIds = new HashMap<>();
    private static final String BASE_URL = "https://be-api-seven.vercel.app/";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public TableDetailsScreen(int tableNumber, List<Order> orders, MainScreen mainScreen) {
        this.orders = orders;
        this.mainScreen = mainScreen;

        initializeMenusFromAPI(() -> setupScreen(tableNumber));
    }

    private void setupScreen(int tableNumber) {
        setTitle("테이블 세부 정보 - Table " + tableNumber);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600); // 전체 창 크기 조정
        setLayout(new BorderLayout(10, 10));

        JPanel orderListPanel = createOrderListPanel(tableNumber);
        JPanel menuPanel = createMenuPanel(tableNumber);
        JPanel buttonPanel = createButtonPanel(tableNumber);

        // JSplitPane 설정
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, orderListPanel, menuPanel);
        centerSplitPane.setResizeWeight(0.4); // 주문 목록 패널에 더 많은 공간 할당
        centerSplitPane.setDividerSize(8); // 구분선 크기 조정
        centerSplitPane.setDividerLocation(400); // 초기 구분 위치 설정

        add(centerSplitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createOrderListPanel(int tableNumber) {
        JPanel orderListPanel = new JPanel(new BorderLayout(10, 10));
        orderListPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));

        // 스크롤 가능한 패널 생성
        JPanel orderItemsPanel = new JPanel();
        orderItemsPanel.setLayout(new BoxLayout(orderItemsPanel, BoxLayout.Y_AXIS));

        int totalAmount = 0;

        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                JPanel itemPanel = new JPanel(new GridLayout(1, 3, 5, 5));
                itemPanel.add(new JLabel(order.getItemName())); // 메뉴 이름
                itemPanel.add(new JLabel("(" + order.getQuantity() + ")")); // 수량
                itemPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원")); // 총 가격

                orderItemsPanel.add(itemPanel); // 항목 추가
                totalAmount += order.getPrice() * order.getQuantity();
            }
        }

        JScrollPane scrollPane = new JScrollPane(orderItemsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JLabel totalLabel = new JLabel("<html>합계: " + totalAmount + "원</html>");
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        // 패널 크기 조정
        orderListPanel.setPreferredSize(new Dimension(350, 500)); // 폭과 높이 조정
        orderListPanel.add(scrollPane, BorderLayout.CENTER);
        orderListPanel.add(totalLabel, BorderLayout.SOUTH);

        return orderListPanel;
    }

    private JPanel createMenuPanel(int tableNumber) {
        JPanel menuPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        menuPanel.setBorder(BorderFactory.createTitledBorder("추가할 메뉴"));

        for (String menu : availableMenus) {
            int price = menuWithPrices.getOrDefault(menu, 0);
            JButton menuButton = new JButton("<html>" + menu + "<br>" + price + "원</html>");
            menuButton.addActionListener(e -> {
                String menuId = menuWithIds.get(menu); // menuId 가져오기
                if (menuId != null) {
                    boolean menuExists = false;

                    // 동일한 메뉴가 있는지 확인
                    for (Order order : orders) {
                        if (order.getTableNumber() == tableNumber && order.getMenuId().equals(menuId)) {
                            order.setQuantity(order.getQuantity() + 1); // 수량 증가
                            menuExists = true;
                            break;
                        }
                    }

                    // 동일한 메뉴가 없으면 새로 추가
                    if (!menuExists) {
                        orders.add(new Order(tableNumber, menuId, menu, 1, price));
                    }

                    System.out.println("현재 orders 상태: " + orders); // 디버깅 출력
                    updateOrderListPanel(tableNumber); // UI 갱신
                } else {
                    JOptionPane.showMessageDialog(this, "선택한 메뉴의 ID를 찾을 수 없습니다: " + menu);
                }
            });

            menuPanel.add(menuButton);
        }

        return menuPanel;
    }

    private JPanel createButtonPanel(int tableNumber) {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 6, 10, 10));

        JButton cancelButton = new JButton("전체 취소");
        cancelButton.addActionListener(e -> {
            clearOrdersFromServer(tableNumber);
        });

        JButton plusButton = new JButton("+");
        plusButton.addActionListener(e -> adjustQuantity(tableNumber, 1));

        JButton minusButton = new JButton("-");
        minusButton.addActionListener(e -> adjustQuantity(tableNumber, -1));

        JButton receiptButton = new JButton("영수증");
        receiptButton.addActionListener(e -> {
            generateReceipt(tableNumber);
            sendTableOrderComplete(tableNumber);
            clearOrdersForTable(tableNumber);
            mainScreen.updateTable(tableNumber, new ArrayList<>(orders));
            dispose();
        });

        JButton reserveButton = new JButton("예약");
        reserveButton.addActionListener(e -> reserveTable(tableNumber));

        JButton orderButton = new JButton("주문하기");
        orderButton.addActionListener(e -> {
            sendOrdersToServer(tableNumber); // 서버로 주문 데이터 전송
            JOptionPane.showMessageDialog(this, "주문이 완료되었습니다!");

            // 주문이 완료된 테이블의 상태 업데이트
            List<Order> tableOrders = new ArrayList<>();
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber) {
                    tableOrders.add(order); // 해당 테이블의 주문 데이터 수집
                }
            }

            // MainScreen에서 해당 테이블만 업데이트
            mainScreen.updateSpecificTable(tableNumber, tableOrders);

            dispose(); // 현재 화면 닫기
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(plusButton);
        buttonPanel.add(minusButton);
        buttonPanel.add(receiptButton);
        buttonPanel.add(reserveButton);
        buttonPanel.add(orderButton);

        return buttonPanel;
    }

    // 주문 초기화 API 호출 메서드
    private void clearOrdersFromServer(int tableNumber) {
        String url = BASE_URL + "api/table?tableNum=" + tableNumber;

        Request request = new Request.Builder()
                .url(url)
                .delete() // DELETE 요청
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            null,
                            "주문 초기화 중 오류가 발생했습니다: " + e.getMessage(),
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
                    boolean success = responseJson.get("success").getAsBoolean();

                    if (success) {
                        SwingUtilities.invokeLater(() -> {
                            // 해당 테이블의 주문 삭제
                            orders.removeIf(order -> order.getTableNumber() == tableNumber);

                            // MainScreen에서 해당 테이블 UI 갱신
                            mainScreen.updateSpecificTable(tableNumber, new ArrayList<>());

                            // 초기화 성공 메시지
                            JOptionPane.showMessageDialog(
                                    null,
                                    responseJson.get("message").getAsString(),
                                    "성공",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // 현재 화면 닫기
                            dispose();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                    null,
                                    "주문 초기화 실패: " + responseJson.get("message").getAsString(),
                                    "오류",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                null,
                                "주문 초기화 실패: 응답을 받지 못했습니다.",
                                "오류",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        });
    }

    private void sendOrdersToServer(int tableNumber) {
        List<Map<String, Object>> orderItems = new ArrayList<>();
        int totalPrice = 0;

        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                Map<String, Object> orderEntry = new HashMap<>();
                orderEntry.put("menuId", order.getMenuId()); // menuId 추가
                orderEntry.put("name", order.getItemName());
                orderEntry.put("quantity", order.getQuantity());
                orderEntry.put("price", order.getPrice());
                orderItems.add(orderEntry);

                totalPrice += order.getPrice() * order.getQuantity();
            }
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderItems", orderItems);
        orderData.put("totalPrice", totalPrice);

        String jsonData = gson.toJson(orderData);
        System.out.println("Generated JSON: " + jsonData);

        RequestBody body = RequestBody.create(jsonData, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(BASE_URL + "api/table/new_order?tableNum=" + tableNumber)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("주문 데이터 전송 실패: " + e.getMessage()); // 터미널에 실패 메시지 출력
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No Response Body";
                System.out.println("Response Code: " + response.code());
                System.out.println("Response Body: " + responseBody);
                if (response.isSuccessful()) {
                    System.out.println("주문이 성공적으로 저장되었습니다! 응답: " + responseBody); // 성공 메시지 출력
                } else {
                    System.err.println("주문 데이터 전송 실패: " + response.code() + " 응답: " + responseBody); // 실패 메시지 출력
                }
            }
        });
    }

    private void refreshScreen(int tableNumber) {
        SwingUtilities.invokeLater(() -> {
            getContentPane().removeAll();
            setupScreen(tableNumber);
            revalidate();
            repaint();
        });
    }

    private void clearOrdersForTable(int tableNumber) {
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
    }

    private void adjustQuantity(int tableNumber, int adjustment) {
        String selectedMenu = (String) JOptionPane.showInputDialog(
                this,
                "수량을 조정할 메뉴를 선택하세요:",
                "수량 조정",
                JOptionPane.PLAIN_MESSAGE,
                null,
                orders.stream().filter(order -> order.getTableNumber() == tableNumber)
                        .map(Order::getItemName).toArray(),
                null);

        if (selectedMenu != null) {
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber && order.getItemName().equals(selectedMenu)) {
                    order.setQuantity(order.getQuantity() + adjustment);
                    if (order.getQuantity() <= 0) {
                        orders.remove(order);
                    }
                    break;
                }
            }
            refreshScreen(tableNumber);
        }
    }

    private void reserveTable(int tableNumber) {
        if (!reservedTables.contains(tableNumber)) {
            mainScreen.reserveTable(tableNumber); // 예약 상태 전달
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "이(가) 예약되었습니다!");
            dispose(); // 화면 닫기
        } else {
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "은(는) 이미 예약되었습니다.");
        }
    }

    // 주문 목록 패널 업데이트 메서드
    private void updateOrderListPanel(int tableNumber) {
        SwingUtilities.invokeLater(() -> {
            JSplitPane splitPane = (JSplitPane) getContentPane().getComponent(0); // 현재 SplitPane 가져오기
            JScrollPane scrollPane = (JScrollPane) ((JPanel) splitPane.getLeftComponent()).getComponent(0); // 기존
                                                                                                            // ScrollPane
                                                                                                            // 가져오기
            JPanel orderItemsPanel = (JPanel) scrollPane.getViewport().getView(); // 기존 Panel 가져오기

            // 기존 항목 삭제 및 다시 추가
            orderItemsPanel.removeAll();

            int totalAmount = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber) {
                    JPanel itemPanel = new JPanel(new GridLayout(1, 3, 5, 5));
                    itemPanel.add(new JLabel(order.getItemName()));
                    itemPanel.add(new JLabel("(" + order.getQuantity() + ")"));
                    itemPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원"));

                    orderItemsPanel.add(itemPanel);
                    totalAmount += order.getPrice() * order.getQuantity();
                }
            }

            // UI 갱신
            orderItemsPanel.revalidate();
            orderItemsPanel.repaint();

            // 총 금액 라벨 갱신
            JLabel totalLabel = new JLabel("<html>합계: " + totalAmount + "원</html>");
            totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            totalLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

            JPanel leftPanel = (JPanel) splitPane.getLeftComponent();
            leftPanel.removeAll();
            leftPanel.add(scrollPane, BorderLayout.CENTER);
            leftPanel.add(totalLabel, BorderLayout.SOUTH);

            leftPanel.revalidate();
            leftPanel.repaint();
        });
    }

    private void initializeMenusFromAPI(Runnable onComplete) {
        Request request = new Request.Builder()
                .url(BASE_URL + "api/menu")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null, "메뉴를 불러오는데 실패했습니다: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("Menu API Response: " + responseBody); // 응답 데이터 디버깅
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray menuArray = jsonObject.getAsJsonArray("menuItems");

                    SwingUtilities.invokeLater(() -> {
                        availableMenus.clear();
                        menuWithPrices.clear();
                        menuWithIds.clear();

                        for (int i = 0; i < menuArray.size(); i++) {
                            JsonObject menuItem = menuArray.get(i).getAsJsonObject();
                            String menuName = menuItem.get("name").getAsString();
                            String menuId = menuItem.get("_id").getAsString(); // `_id` 필드로 수정
                            int menuPrice = menuItem.get("price").getAsInt();

                            availableMenus.add(menuName);
                            menuWithPrices.put(menuName, menuPrice);
                            menuWithIds.put(menuName, menuId); // menuWithIds에 저장
                        }

                        System.out.println("Available Menus: " + availableMenus); // 메뉴 디버깅
                        System.out.println("Menu IDs: " + menuWithIds); // menuId 매핑 디버깅

                        onComplete.run();
                    });
                } else {
                    System.out.println("Menu API Response Failed: " + response.message());
                }
            }
        });
    }

    private void generateReceipt(int tableNumber) {
        try {
            // PDF 파일 저장 경로 설정
            String directoryPath = "./Receipt/";
            String fileName = directoryPath + "Receipt_Table_" + tableNumber + ".pdf";

            // 디렉토리 존재 여부 확인 및 생성
            java.io.File directory = new java.io.File(directoryPath);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    JOptionPane.showMessageDialog(this, "디렉토리를 생성할 수 없습니다: " + directoryPath);
                    return;
                }
            }

            PdfWriter writer = new PdfWriter(fileName);
            PdfDocument pdf = new PdfDocument(writer);

            // Document 생성
            Document document = new Document(pdf);

            // 현재 날짜와 시간 가져오기
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 헤더 추가
            document.add(new Paragraph("Restaurant Receipt").setBold().setFontSize(18)
                    .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER));
            document.add(new Paragraph("Date: " + dateTime)
                    .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT));
            document.add(new Paragraph("Table: " + tableNumber)
                    .setTextAlignment(com.itextpdf.layout.property.TextAlignment.LEFT));
            document.add(new Paragraph("----------------------"));

            // 테이블 형식으로 영수증 데이터 추가
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(3);
            table.addCell(new Paragraph("Item").setBold());
            table.addCell(new Paragraph("Quantity").setBold());
            table.addCell(new Paragraph("Price").setBold());

            int totalAmount = 0;
            for (Order order : orders) {
                if (order.getTableNumber() == tableNumber) {
                    table.addCell(order.getItemName());
                    table.addCell(String.valueOf(order.getQuantity()));
                    table.addCell(String.format("%d 원", order.getQuantity() * order.getPrice()));
                    totalAmount += order.getQuantity() * order.getPrice();
                }
            }

            document.add(table);
            document.add(new Paragraph("----------------------"));

            // 총 합계 표시
            document.add(new Paragraph("Total: " + totalAmount + " 원").setBold().setFontSize(14)
                    .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT));

            // Document 닫기
            document.close();

            // PDF 생성 성공 메시지
            JOptionPane.showMessageDialog(this, "Receipt has been saved: " + fileName);
        } catch (Exception e) {
            // 오류 메시지 표시
            JOptionPane.showMessageDialog(this, "Error generating receipt: " + e.getMessage());
        }
    }

    // 테이블 주문 완료 API 호출 메서드 추가
    private void sendTableOrderComplete(int tableNumber) {
        // API URL 설정
        String apiUrl = BASE_URL + "api/table?tableNum=" + tableNumber;

        // 요청 데이터 설정 (필요 시 추가 데이터 포함 가능)
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("tableNum", tableNumber); // 테이블 번호를 요청 데이터로 포함

        String jsonData = gson.toJson(requestData);

        // 요청 본문 생성
        RequestBody body = RequestBody.create(jsonData, MediaType.get("application/json"));

        // 요청 생성
        Request request = new Request.Builder()
                .url(apiUrl)
                .delete(body) // DELETE 메서드 사용
                .build();

        // 비동기 호출
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("테이블 주문 삭제 실패: " + e.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null, "테이블 주문 데이터 전송 실패: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No Response Body";
                System.out.println("API 응답 코드: " + response.code());
                System.out.println("API 응답 내용: " + responseBody);

                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccessful()) {
                        JOptionPane.showMessageDialog(null, "테이블 결게가 완료 돼었습니다!");
                    } else {
                        JOptionPane.showMessageDialog(null, "테이블 주문 데이터 삭제 실패: " + response.message());
                    }
                });
            }
        });
    }
}
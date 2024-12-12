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
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        JPanel orderListPanel = createOrderListPanel(tableNumber);
        JPanel menuPanel = createMenuPanel(tableNumber);
        JPanel buttonPanel = createButtonPanel(tableNumber);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, orderListPanel, menuPanel);
        centerSplitPane.setResizeWeight(0.5);
        centerSplitPane.setDividerSize(5);

        add(centerSplitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createOrderListPanel(int tableNumber) {
        JPanel orderListPanel = new JPanel(new BorderLayout(10, 10));
        orderListPanel.setBorder(BorderFactory.createTitledBorder("주문 목록"));

        JPanel orderItemsPanel = new JPanel(new GridLayout(0, 3, 10, 10)); // 주문 항목
        int totalAmount = 0;

        for (Order order : orders) {
            if (order.getTableNumber() == tableNumber) {
                // 주문 정보를 패널에 추가
                orderItemsPanel.add(new JLabel(order.getItemName()));
                orderItemsPanel.add(new JLabel("(" + order.getQuantity() + ")"));
                orderItemsPanel.add(new JLabel(order.getPrice() * order.getQuantity() + "원"));
                totalAmount += order.getPrice() * order.getQuantity();
            }
        }

        JLabel totalLabel = new JLabel("합계: " + totalAmount + "원"); // 총 금액
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));

        orderListPanel.add(orderItemsPanel, BorderLayout.CENTER);
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
                if (menuId != null) { // menuId가 존재하는지 확인
                    System.out.println("Selected Menu: " + menu + ", Menu ID: " + menuId); // 디버깅 출력
                    orders.add(new Order(tableNumber, menuId, menu, 1, price)); // menuId 포함 생성자 사용
                    refreshScreen(tableNumber);
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
            clearOrdersForTable(tableNumber);
            refreshScreen(tableNumber);
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
            sendOrdersToServer(tableNumber);
            JOptionPane.showMessageDialog(this, "주문이 완료되었습니다!");
            mainScreen.updateTable(tableNumber, new ArrayList<>(orders));
            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(plusButton);
        buttonPanel.add(minusButton);
        buttonPanel.add(receiptButton);
        buttonPanel.add(reserveButton);
        buttonPanel.add(orderButton);

        return buttonPanel;
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
            reservedTables.add(tableNumber);
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "이(가) 예약되었습니다!");
        } else {
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + "은(는) 이미 예약되었습니다.");
        }
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
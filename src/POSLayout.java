import components.MainScreen;
import components.FunctionPanel;
import models.Order;

import javax.swing.*;
import com.google.gson.Gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class POSLayout extends JFrame {
    private final List<Order> orders;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private MainScreen mainScreen; // 클래스 변수로 선언
    private static final String BASE_URL = "https://be-api-seven.vercel.app/";

    public POSLayout() {
        setTitle("POS System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // 주문 데이터 초기화
        orders = new ArrayList<>();

        // MainScreen 초기화
        mainScreen = new MainScreen(orders);
        add(mainScreen, BorderLayout.CENTER);

        // FunctionPanel 추가
        FunctionPanel functionPanel = new FunctionPanel(orders, mainScreen);
        add(functionPanel, BorderLayout.EAST);

        // API 호출로 초기 데이터 가져오기
        initializeOrders();

        setVisible(true);
    }

    private void initializeOrders() {
        Request request = new Request.Builder()
                .url(BASE_URL + "api/table")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null, "주문 데이터를 불러오지 못했습니다: " + e.getMessage()));
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray tableData = jsonObject.getAsJsonArray("tables");

                    SwingUtilities.invokeLater(() -> {
                        orders.clear(); // 기존 데이터 초기화

                        // 테이블 데이터 처리
                        for (JsonElement element : tableData) {
                            JsonObject tableObject = element.getAsJsonObject();
                            int tableNum = tableObject.get("tableNum").getAsInt();
                            JsonArray lastOrder = tableObject.getAsJsonArray("lastOrder");

                            if (lastOrder != null) {
                                for (JsonElement orderElement : lastOrder) {
                                    JsonObject orderObject = orderElement.getAsJsonObject();
                                    String itemName = orderObject.get("name").getAsString();
                                    int quantity = orderObject.get("quantity").getAsInt();
                                    int price = orderObject.get("price").getAsInt();

                                    // `orders`에 추가
                                    orders.add(new Order(tableNum, itemName, quantity, price));
                                }
                            }
                        }

                        // UI 갱신
                        if (mainScreen != null) {
                            mainScreen.updateTable(-1, new ArrayList<>(orders)); // 전체 갱신
                        }
                    });
                } else {
                    System.out.println("API 응답 실패: " + response.message());
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(POSLayout::new);
    }
}
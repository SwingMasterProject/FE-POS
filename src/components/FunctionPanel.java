package components;

import models.Employee;
import models.Order;

import javax.swing.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.*;

public class FunctionPanel extends JPanel {
    private final List<String> availableMenus; // 추가 가능한 메뉴 목록
    private final Map<String, Integer> menuWithPrices = new HashMap<>();
    private final List<Employee> employees = new ArrayList<>();
    private final List<Order> orders; // 주문 데이터를 저장하는 리스트
    private static final String BASE_URL = "https://be-api-seven.vercel.app/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Gson gson = new Gson();

    public FunctionPanel(List<Order> orders, MainScreen mainScreen) {
        this.orders = orders;
        availableMenus = new ArrayList<>();
        setLayout(new GridLayout(4, 1, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] functionNames = { "직원 관리", "현재 인기 메뉴", "현재 매출액", "요청 사항" };
        for (String name : functionNames) {
            JButton functionButton = new JButton(name);
            functionButton.addActionListener(e -> handleFunction(name, orders));
            add(functionButton);
        }
    }

    private void handleFunction(String name, List<Order> orderList) {
        switch (name) {
            case "직원 관리":
                manageEmployees();
                break;
            case "현재 인기 메뉴":
                showPopularMenu(orders);
                break;
            case "현재 매출액":
                showTotalSales(orders);
                break;
            case "요청 사항":
                showAllRequests();
                break;
            default:
                JOptionPane.showMessageDialog(this, "알 수 없는 기능입니다.");
        }
    }

    private void showAllRequests() {
        Request request = new Request.Builder()
                .url(BASE_URL + "api/request")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(null, "요청사항 데이터를 불러오는데 실패했습니다: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray requestArray = jsonResponse.getAsJsonArray("data");

                    SwingUtilities.invokeLater(() -> {
                        if (requestArray == null || requestArray.size() == 0) {
                            displayEmptyRequestsUI(); // 요청사항이 없을 경우 처리
                        } else {
                            displayRequestsUI(requestArray); // 요청사항 표시
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "요청사항 데이터를 불러오는데 실패했습니다: " + response.message()));
                }
            }
        });
    }

    private void displayEmptyRequestsUI() {
        JFrame emptyRequestFrame = new JFrame("요청사항 리스트");
        emptyRequestFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        emptyRequestFrame.setSize(500, 400);

        JLabel emptyMessageLabel = new JLabel("<html>요청사항이 없습니다.</html>", SwingConstants.CENTER);
        emptyMessageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        emptyRequestFrame.add(emptyMessageLabel);

        emptyRequestFrame.setVisible(true);
    }

    private void displayRequestsUI(JsonArray requestArray) {
        JFrame requestFrame = new JFrame("요청사항 리스트");
        requestFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        requestFrame.setSize(500, 400);
        requestFrame.setLayout(new BorderLayout());

        JPanel requestListPanel = new JPanel();
        requestListPanel.setLayout(new BoxLayout(requestListPanel, BoxLayout.Y_AXIS));

        for (JsonElement requestElement : requestArray) {
            JsonObject requestObj = requestElement.getAsJsonObject();
            int tableNum = requestObj.get("tableNum").getAsInt();
            JsonArray requests = requestObj.getAsJsonArray("requests");
            boolean isCompleted = requestObj.get("isCompleted").getAsBoolean();

            for (JsonElement element : requests) {
                JsonObject requestDetail = element.getAsJsonObject();
                String requestId = requestObj.get("_id").getAsString();
                String requestName = requestDetail.get("name").getAsString();

                JPanel requestItemPanel = new JPanel(new BorderLayout());
                requestItemPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JLabel requestLabel = new JLabel("Table " + tableNum + ": " + requestName);
                JButton completeButton = new JButton(isCompleted ? "완료됨" : "완료");
                completeButton.setEnabled(!isCompleted);

                completeButton.addActionListener(e -> updateRequestStatus(requestId, completeButton, requestItemPanel));

                requestItemPanel.add(requestLabel, BorderLayout.CENTER);
                requestItemPanel.add(completeButton, BorderLayout.EAST);

                requestListPanel.add(requestItemPanel);
            }
        }

        JScrollPane scrollPane = new JScrollPane(requestListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        requestFrame.add(scrollPane, BorderLayout.CENTER);
        requestFrame.setVisible(true);
    }

    private void updateRequestStatus(String requestId, JButton completeButton, JPanel requestItemPanel) {
        RequestBody body = RequestBody.create("", MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(BASE_URL + "api/request?id=" + requestId)
                .put(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities
                        .invokeLater(() -> JOptionPane.showMessageDialog(null, "요청 완료 상태 업데이트 실패: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    SwingUtilities.invokeLater(() -> {
                        completeButton.setEnabled(false);
                        requestItemPanel.setBackground(Color.LIGHT_GRAY);
                        completeButton.setText("완료됨");
                    });
                } else {
                    SwingUtilities.invokeLater(
                            () -> JOptionPane.showMessageDialog(null, "요청 완료 상태 업데이트 실패: " + response.message()));
                }
            }
        });
    }

    private void showPopularMenu(List<Order> orderList) {
        Map<String, Integer> menuCount = new HashMap<>();
        for (Order order : orders) {
            menuCount.put(order.getItemName(),
                    menuCount.getOrDefault(order.getItemName(), 0) + order.getQuantity());
        }
        String popularMenu = menuCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No Orders");
        JOptionPane.showMessageDialog(this, "현재 인기 메뉴: " + popularMenu);
    }

    private void showTotalSales(List<Order> orderList) {
        int totalSales = orders.stream()
                .mapToInt(order -> order.getQuantity() * order.getPrice())
                .sum();
        JOptionPane.showMessageDialog(this, "현재 매출액: " + totalSales + "원");
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

    // viewEmployeeRecords 수정
    private void viewEmployeeRecords() {
        Request request = new Request.Builder()
                .url(BASE_URL + "api/time-records")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(
                        () -> JOptionPane.showMessageDialog(null, "직원 데이터를 불러오는데 실패했습니다: " + e.getMessage()));
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
                    SwingUtilities.invokeLater(
                            () -> JOptionPane.showMessageDialog(null, "직원 데이터를 불러오는데 실패했습니다: " + response.message()));
                }
            }
        });
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
}
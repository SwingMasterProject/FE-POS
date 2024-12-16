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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private JTextArea employeeListArea;

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
                EmployeeManager employeeManager = new EmployeeManager();
                employeeManager.manageEmployees();
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

    public class EmployeeManager {
        private static final String BASE_URL = "https://be-api-seven.vercel.app/";
        private static final String RECORDS_URL = "https://be-api-takaaaans-projects.vercel.app/api/time-records";
        private static final OkHttpClient httpClient = new OkHttpClient();
        private static final Gson gson = new Gson();
        private JTextArea employeeListArea;
        private JTextArea attendanceRecordsArea;

        public void manageEmployees() {
            JFrame frame = new JFrame("직원 관리");
            frame.setSize(600, 800);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel inputPanel = new JPanel();
            JTextField nameField = new JTextField(20);
            String[] actions = { "직원 등록", "출근", "퇴근", "출퇴근 기록 조회" };
            JComboBox<String> actionSelector = new JComboBox<>(actions);
            JButton actionButton = new JButton("확인");

            inputPanel.add(new JLabel("이름:"));
            inputPanel.add(nameField);
            inputPanel.add(actionSelector);
            inputPanel.add(actionButton);

            employeeListArea = new JTextArea(10, 40);
            attendanceRecordsArea = new JTextArea(15, 40);
            JScrollPane employeeScroll = new JScrollPane(employeeListArea);
            JScrollPane attendanceScroll = new JScrollPane(attendanceRecordsArea);

            employeeListArea.setEditable(false);
            attendanceRecordsArea.setEditable(false);

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.add(new JLabel("직원 리스트"));
            listPanel.add(employeeScroll);
            listPanel.add(new JLabel("출퇴근 기록"));
            listPanel.add(attendanceScroll);

            frame.add(inputPanel, BorderLayout.NORTH);
            frame.add(listPanel, BorderLayout.CENTER);

            actionButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String action = (String) actionSelector.getSelectedItem();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "이름을 입력해주세요.");
                    return;
                }

                switch (action) {
                    case "직원 등록":
                        registerEmployee(name);
                        break;
                    case "출근":
                        handleClockAction(name, "clock-in");
                        break;
                    case "퇴근":
                        handleClockAction(name, "clock-out");
                        break;
                    case "출퇴근 기록 조회":
                        fetchAttendanceRecordsByName(name, attendanceRecordsArea);
                        break;
                }
            });

            updateEmployeeList();
            frame.setVisible(true);
        }

        private void registerEmployee(String name) {
            Request request = new Request.Builder()
                    .url(BASE_URL + "api/user?name=" + name)
                    .post(RequestBody.create("", MediaType.get("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showError("직원 등록 실패", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, name + "님이 등록되었습니다.");
                            updateEmployeeList();
                        });
                    } else {
                        showError("직원 등록 실패", response.message());
                    }
                }
            });
        }

        private void handleClockAction(String name, String action) {
            fetchUserId(name, userId -> {
                String url = BASE_URL + "api/time-records/" + action + "?user_id=" + userId;
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create("", MediaType.get("application/json")))
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showError(action.equals("clock-in") ? "출근 실패" : "퇴근 실패", e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                                    action.equals("clock-in") ? "출근 성공" : "퇴근 성공"));
                        } else {
                            showError(action.equals("clock-in") ? "출근 실패" : "퇴근 실패", response.message());
                        }
                    }
                });
            });
        }

        private void fetchAttendanceRecordsByName(String name, JTextArea attendanceRecordsArea) {
            fetchUserId(name, userId -> {
                if (userId == null || userId.isEmpty()) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "해당 이름의 직원 ID를 찾을 수 없습니다."));
                    return;
                }

                String startDate = "2024-01-01"; // 기본 시작 날짜
                String endDate = "2024-12-31"; // 기본 종료 날짜
                String url = BASE_URL + "api/time-records?start_date=" + startDate + "T00:00:00.000Z"
                        + "&end_date=" + endDate + "T23:59:59.000Z&user_id=" + userId;

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        SwingUtilities.invokeLater(
                                () -> attendanceRecordsArea.setText("출퇴근 기록을 불러오는데 실패했습니다: " + e.getMessage()));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();
                        System.out.println("Attendance Records Response: " + responseBody); // 디버깅용

                        if (response.isSuccessful()) {
                            try {
                                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                                JsonArray records = jsonResponse.getAsJsonArray("records");

                                if (records == null || records.size() == 0) {
                                    SwingUtilities.invokeLater(() -> attendanceRecordsArea.setText("출퇴근 기록이 없습니다."));
                                    return;
                                }

                                StringBuilder recordDetails = new StringBuilder("출퇴근 기록:\n");

                                for (JsonElement recordElement : records) {
                                    JsonObject record = recordElement.getAsJsonObject();
                                    String clockIn = "출근 기록 없음";
                                    String clockOut = "퇴근 기록 없음";
                                    double workingHours = 0.0;

                                    // clock_in 처리
                                    if (record.has("clock_in") && !record.get("clock_in").isJsonNull()) {
                                        clockIn = convertToSeoulTime(record.get("clock_in").getAsString());
                                    }

                                    // clock_out 처리
                                    if (record.has("clock_out") && !record.get("clock_out").isJsonNull()) {
                                        clockOut = convertToSeoulTime(record.get("clock_out").getAsString());
                                    }

                                    // total_working_hours 처리
                                    if (record.has("total_working_hours")
                                            && !record.get("total_working_hours").isJsonNull()) {
                                        workingHours = record.get("total_working_hours").getAsDouble();
                                    }

                                    recordDetails.append("출근: ").append(clockIn)
                                            .append(", 퇴근: ").append(clockOut)
                                            .append(", 근무 시간: ").append(String.format("%.2f", workingHours))
                                            .append("시간")
                                            .append("\n");
                                }

                                double totalHours = jsonResponse.has("total_working_hours")
                                        && !jsonResponse.get("total_working_hours").isJsonNull()
                                                ? jsonResponse.get("total_working_hours").getAsDouble()
                                                : 0.0;

                                recordDetails.append("\n총 근무 시간: ").append(String.format("%.2f", totalHours))
                                        .append("시간");

                                SwingUtilities
                                        .invokeLater(() -> attendanceRecordsArea.setText(recordDetails.toString()));
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(
                                        () -> attendanceRecordsArea.setText("응답 처리 중 오류 발생: " + e.getMessage()));
                            }
                        } else {
                            SwingUtilities.invokeLater(
                                    () -> attendanceRecordsArea.setText("출퇴근 기록 조회 실패: " + response.message()));
                        }
                    }
                });
            });
        }

        private String convertToSeoulTime(String utcTime) {
            try {
                // UTC 시간 문자열을 파싱
                Instant instant = Instant.parse(utcTime); // 예: "2024-12-17T12:00:00Z"
                ZonedDateTime seoulTime = instant.atZone(ZoneId.of("Asia/Seoul")); // 서울 시간으로 변환
                return seoulTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // 원하는 포맷으로 출력
            } catch (Exception e) {
                e.printStackTrace();
                return "시간 변환 오류";
            }
        }

        private void fetchUserId(String name, java.util.function.Consumer<String> callback) {
            String url = BASE_URL + "api/user";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    SwingUtilities
                            .invokeLater(() -> JOptionPane.showMessageDialog(null, "유저 ID 조회 실패: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    System.out.println("fetchUserId response: " + responseBody); // 디버깅 메시지

                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                            // 응답 데이터에서 `users` 배열 확인
                            if (jsonResponse.has("users") && jsonResponse.get("users").isJsonArray()) {
                                JsonArray users = jsonResponse.getAsJsonArray("users");

                                // `users` 배열에서 이름에 해당하는 데이터를 찾음
                                for (JsonElement userElement : users) {
                                    JsonObject user = userElement.getAsJsonObject();
                                    String userName = user.get("name").getAsString();

                                    if (userName.equals(name)) {
                                        String userId = user.get("_id").getAsString();
                                        SwingUtilities.invokeLater(() -> callback.accept(userId));
                                        return;
                                    }
                                }

                                // 이름에 해당하는 데이터가 없을 경우
                                SwingUtilities.invokeLater(
                                        () -> JOptionPane.showMessageDialog(null, "해당 이름의 사용자를 찾을 수 없습니다."));
                            } else {
                                SwingUtilities
                                        .invokeLater(() -> JOptionPane.showMessageDialog(null, "사용자 데이터가 잘못되었습니다."));
                            }
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(
                                    () -> JOptionPane.showMessageDialog(null, "응답 파싱 중 오류 발생: " + e.getMessage()));
                        }
                    } else {
                        SwingUtilities.invokeLater(
                                () -> JOptionPane.showMessageDialog(null, "유저 ID 조회 실패: " + response.message()));
                    }
                }
            });
        }

        private void updateEmployeeList() {
            Request request = new Request.Builder()
                    .url(BASE_URL + "api/user")
                    .get()
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showError("직원 리스트 업데이트 실패", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray users = jsonResponse.getAsJsonArray("users");

                        if (users != null && users.size() > 0) {
                            StringBuilder list = new StringBuilder();
                            for (JsonElement userElement : users) {
                                JsonObject user = userElement.getAsJsonObject();
                                String userName = user.get("name").getAsString();
                                list.append(userName).append("\n");
                            }
                            SwingUtilities.invokeLater(() -> employeeListArea.setText(list.toString()));
                        } else {
                            SwingUtilities.invokeLater(() -> employeeListArea.setText("등록된 직원이 없습니다."));
                        }
                    } else {
                        showError("직원 리스트 업데이트 실패", response.message());
                    }
                }
            });
        }

        private void showError(String title, String message) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, title + ": " + message));
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
}
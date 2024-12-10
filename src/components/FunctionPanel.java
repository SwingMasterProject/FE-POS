package components;

import models.Employee;
import models.Order;

import javax.swing.*;

import com.google.gson.Gson;

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

    public FunctionPanel(
            List<Order> orders, MainScreen mainScreen) {
        this.orders = orders;
        availableMenus = new ArrayList<>();
        setLayout(new GridLayout(5, 1, 5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] functionNames = { "직원 관리", "메뉴 편집", "일일 인기 메뉴", "총 매출액", "요청 사항" };
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
            case "메뉴 편집":
                editMenu();
                break;
            case "일일 인기 메뉴":
                showPopularMenu(orders);
                break;
            case "총 매출액":
                showTotalSales(orders);
                break;
            case "요청 사항":
                JOptionPane.showMessageDialog(this, "요청 사항 기능은 현재 준비 중입니다!");
                break;
            default:
                JOptionPane.showMessageDialog(this, "알 수 없는 기능입니다.");
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
        JOptionPane.showMessageDialog(this, "오늘의 인기 메뉴: " + popularMenu);
    }

    private void showTotalSales(List<Order> orderList) {
        int totalSales = orders.stream()
                .mapToInt(order -> order.getQuantity() * order.getPrice())
                .sum();
        JOptionPane.showMessageDialog(this, "총 매출액: " + totalSales + "원");
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
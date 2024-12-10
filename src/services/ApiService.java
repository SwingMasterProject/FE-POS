package services;

import com.google.gson.Gson;
import okhttp3.*;
import javax.swing.*;
import java.io.IOException;

public class ApiService {
    private static final String BASE_URL = "https://be-api-seven.vercel.app/";
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Gson gson = new Gson();

    public static void fetchMenus(JPanel callbackPanel) {
        Request request = new Request.Builder()
                .url(BASE_URL + "api/menu")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(callbackPanel, "메뉴 가져오기 실패: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("Response: " + responseBody); // 디버깅용
                    // 추가 로직 처리...
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(callbackPanel, "메뉴 가져오기 실패: " + response.message()));
                }
            }
        });
    }
}

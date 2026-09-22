package com.medibook.app;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.medibook.app.common.models.KetQuaDangNhap;
import com.medibook.app.common.network.SupabaseApi;
import com.medibook.app.common.network.SupabaseClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtMatKhau;
    private Button btnDangNhap;
    private TextView tvKetQua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtMatKhau = findViewById(R.id.edtMatKhau);
        btnDangNhap = findViewById(R.id.btnDangNhap);
        tvKetQua = findViewById(R.id.tvKetQua);

        btnDangNhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String matKhau = edtMatKhau.getText().toString().trim();

                if (email.isEmpty() || matKhau.isEmpty()) {
                    tvKetQua.setText("Vui lòng nhập đầy đủ Email và Mật khẩu");
                    return;
                }

                tvKetQua.setText("Đang đăng nhập...");
                thucHienDangNhap(email, matKhau);
            }
        });

        TextView tvChuaCoTaiKhoan = findViewById(R.id.tvChuaCoTaiKhoan);
        tvChuaCoTaiKhoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void thucHienDangNhap(String email, String matKhau) {
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", matKhau);

        Call<KetQuaDangNhap> call = api.dangNhap(body);
        call.enqueue(new Callback<KetQuaDangNhap>() {
            @Override
            public void onResponse(Call<KetQuaDangNhap> call, Response<KetQuaDangNhap> response) {
                if (response.isSuccessful() && response.body() != null) {
                    KetQuaDangNhap ketQua = response.body();
                    String userId = ketQua.getUser().getId();
                    String accessToken = ketQua.getAccessToken();

                    // Lưu tạm thông tin đăng nhập vào bộ nhớ máy (SharedPreferences)
                    SharedPreferences prefs = getSharedPreferences("MediBookPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_id", userId);
                    editor.putString("access_token", accessToken);
                    editor.apply();

                    tvKetQua.setTextColor(0xFF00AA00); // đổi màu chữ thành xanh lá
                    tvKetQua.setText("Đăng nhập thành công! User ID: " + userId);
                } else {
                    tvKetQua.setText("Sai email hoặc mật khẩu. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<KetQuaDangNhap> call, Throwable t) {
                tvKetQua.setText("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
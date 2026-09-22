package com.medibook.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.medibook.app.common.models.HoSo;
import com.medibook.app.common.models.KetQuaDangNhap;
import com.medibook.app.common.network.SupabaseApi;
import com.medibook.app.common.network.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtHoTen, edtEmail, edtMatKhau, edtXacNhanMatKhau;
    private Button btnDangKy;
    private TextView tvKetQua, tvDaCoTaiKhoan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtHoTen = findViewById(R.id.edtHoTen);
        edtEmail = findViewById(R.id.edtEmail);
        edtMatKhau = findViewById(R.id.edtMatKhau);
        edtXacNhanMatKhau = findViewById(R.id.edtXacNhanMatKhau);
        btnDangKy = findViewById(R.id.btnDangKy);
        tvKetQua = findViewById(R.id.tvKetQua);
        tvDaCoTaiKhoan = findViewById(R.id.tvDaCoTaiKhoan);

        // Bấm "Đã có tài khoản? Đăng nhập ngay" -> quay lại LoginActivity
        tvDaCoTaiKhoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnDangKy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hoTen = edtHoTen.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String matKhau = edtMatKhau.getText().toString().trim();
                String xacNhan = edtXacNhanMatKhau.getText().toString().trim();

                if (hoTen.isEmpty() || email.isEmpty() || matKhau.isEmpty() || xacNhan.isEmpty()) {
                    tvKetQua.setText("Vui lòng nhập đầy đủ thông tin");
                    return;
                }
                if (matKhau.length() < 8) {
                    tvKetQua.setText("Mật khẩu phải có ít nhất 8 ký tự");
                    return;
                }
                if (!matKhau.equals(xacNhan)) {
                    tvKetQua.setText("Mật khẩu xác nhận không khớp");
                    return;
                }

                tvKetQua.setTextColor(0xFF000000);
                tvKetQua.setText("Đang đăng ký...");
                thucHienDangKy(hoTen, email, matKhau);
            }
        });
    }

    private void thucHienDangKy(String hoTen, String email, String matKhau) {
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", matKhau);

        api.dangKy(body).enqueue(new Callback<KetQuaDangNhap>() {
            @Override
            public void onResponse(Call<KetQuaDangNhap> call, Response<KetQuaDangNhap> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String userId = response.body().getUser().getId();
                    taoHoSo(userId, hoTen);
                } else {
                    String loiThat = "";
                    try {
                        loiThat = response.errorBody().string();
                    } catch (Exception e) {}
                    tvKetQua.setTextColor(0xFFFF0000);
                    tvKetQua.setText("Lỗi: " + response.code() + " - " + loiThat);
                }
            }

            @Override
            public void onFailure(Call<KetQuaDangNhap> call, Throwable t) {
                tvKetQua.setTextColor(0xFFFF0000);
                tvKetQua.setText("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void taoHoSo(String userId, String hoTen) {
        SupabaseApi api = SupabaseClient.getClient().create(SupabaseApi.class);

        HoSo hoSo = new HoSo();
        hoSo.setId(userId);
        hoSo.setHoTen(hoTen);
        hoSo.setVaiTro("benh_nhan");

        api.taoHoSo(hoSo).enqueue(new Callback<List<HoSo>>() {
            @Override
            public void onResponse(Call<List<HoSo>> call, Response<List<HoSo>> response) {
                if (response.isSuccessful()) {
                    tvKetQua.setTextColor(0xFF00AA00);
                    tvKetQua.setText("Đăng ký thành công! Đang chuyển đến trang đăng nhập...");

                    // Sau 1.5 giây, tự động chuyển sang màn hình Đăng nhập
                    tvKetQua.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        }
                    }, 1500);
                } else {
                    tvKetQua.setTextColor(0xFFFF0000);
                    tvKetQua.setText("Tạo tài khoản thành công nhưng lỗi lưu hồ sơ. Liên hệ hỗ trợ.");
                }
            }

            @Override
            public void onFailure(Call<List<HoSo>> call, Throwable t) {
                tvKetQua.setTextColor(0xFFFF0000);
                tvKetQua.setText("Lỗi lưu hồ sơ: " + t.getMessage());
            }
        });
    }
}
package com.medibook.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnBenhNhan, btnBacSi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ 2 nút từ activity_main.xml
        btnBenhNhan = findViewById(R.id.btnBenhNhan);
        btnBacSi = findViewById(R.id.btnBacSi);

        // Khi bấm "Tôi là Bệnh nhân" → CHUYỂN SANG màn hình LoginActivity
        btnBenhNhan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Khi bấm "Tôi là Bác sĩ" → CŨNG chuyển sang LoginActivity (dùng chung màn hình đăng nhập)
        btnBacSi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
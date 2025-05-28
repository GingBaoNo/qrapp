package com.example.qrapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private QrHistoryAdapter adapter;
    private HistoryViewModel historyViewModel;
    private TextView emptyView; // TextView để hiển thị khi danh sách trống

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        emptyView = findViewById(R.id.emptyView); // Khởi tạo emptyView

        // Cấu hình RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QrHistoryAdapter(new ArrayList<>()); // Khởi tạo với danh sách rỗng
        recyclerView.setAdapter(adapter);

        // Khởi tạo ViewModel
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // Quan sát LiveData từ ViewModel
        historyViewModel.getAllScans().observe(this, qrCodeScans -> {
            if (qrCodeScans != null && !qrCodeScans.isEmpty()) {
                adapter.setScanList(qrCodeScans);
                emptyView.setVisibility(android.view.View.GONE); // Ẩn TextView khi có dữ liệu
                recyclerView.setVisibility(android.view.View.VISIBLE); // Hiển thị RecyclerView
            } else {
                adapter.setScanList(new ArrayList<>()); // Xóa danh sách nếu không có gì
                emptyView.setVisibility(android.view.View.VISIBLE); // Hiển thị TextView khi không có dữ liệu
                recyclerView.setVisibility(android.view.View.GONE); // Ẩn RecyclerView
            }
        });
    }
}
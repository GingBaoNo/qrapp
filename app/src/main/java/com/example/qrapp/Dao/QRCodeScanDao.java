package com.example.qrapp.Dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.qrapp.Entity.QRCodeScan;

import java.util.List;

// @Dao: Đánh dấu interface này là Data Access Object
@Dao
public interface QRCodeScanDao {
    // @Insert: Annotation để chèn dữ liệu vào bảng
    @Insert
    void insert(QRCodeScan qrCodeScan);

    // @Query: Annotation để viết các truy vấn SQL tùy chỉnh
    // LiveData<List<QRCodeScan>>: Khi có dữ liệu mới, LiveData sẽ tự động thông báo để cập nhật UI
    @Query("SELECT * FROM qr_code_scans ORDER BY timestamp DESC")
    LiveData<List<QRCodeScan>> getAllScans(); // Lấy tất cả các bản quét, sắp xếp theo thời gian mới nhất

    @Query("DELETE FROM qr_code_scans")
    void deleteAllScans(); // Xóa tất cả các bản ghi (ví dụ cho mục đích debug)
}
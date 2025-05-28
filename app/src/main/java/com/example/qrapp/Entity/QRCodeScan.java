package com.example.qrapp.Entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// @Entity: Đánh dấu class này là một thực thể trong database, và tên bảng là "qr_code_scans"
@Entity(tableName = "qr_code_scans")
public class QRCodeScan {
    // @PrimaryKey: Đánh dấu id là khóa chính. autoGenerate = true tự động tăng giá trị id.
    @PrimaryKey(autoGenerate = true)
    public int id;

    // @ColumnInfo: Định nghĩa tên cột trong database.
    @ColumnInfo(name = "image_path")
    public String imagePath; // Đường dẫn URI của ảnh QR đã quét (lưu dưới dạng String)

    @ColumnInfo(name = "scanned_content")
    public String scannedContent; // Nội dung của QR code

    @ColumnInfo(name = "timestamp")
    public long timestamp; // Thời gian quét (dạng timestamp long)

    // Constructor để Room có thể tạo đối tượng khi đọc từ DB
    public QRCodeScan(String imagePath, String scannedContent, long timestamp) {
        this.imagePath = imagePath;
        this.scannedContent = scannedContent;
        this.timestamp = timestamp;
    }

    // Getters (Room sẽ dùng các getters để đọc dữ liệu)
    public int getId() { return id; }
    public String getImagePath() { return imagePath; }
    public String getScannedContent() { return scannedContent; }
    public long getTimestamp() { return timestamp; }

    // Setter cho id (nếu cần thiết, Room thường tự gán khi autoGenerate = true)
    public void setId(int id) { this.id = id; }
}
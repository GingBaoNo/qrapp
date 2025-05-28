package com.example.qrapp.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.qrapp.Dao.QRCodeScanDao;
import com.example.qrapp.Entity.QRCodeScan;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// @Database: Đánh dấu class này là database.
// entities: Liệt kê các Entity mà database này quản lý.
// version: Phiên bản của database (quan trọng khi bạn cần nâng cấp database).
// exportSchema: Có nên xuất schema ra file để kiểm tra không.
@Database(entities = {QRCodeScan.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    // Phương thức trừu tượng để truy cập DAO của bạn
    public abstract QRCodeScanDao qrCodeScanDao();

    // Singleton instance của database để tránh nhiều instance mở database cùng lúc
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    // ExecutorService để thực hiện các thao tác database trên một thread riêng (không phải UI thread)
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Phương thức để lấy instance của database (Singleton pattern)
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            // Đồng bộ hóa để đảm bảo chỉ có một thread khởi tạo database
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), // Context của ứng dụng
                                    AppDatabase.class, // Class database của bạn
                                    "qr_scan_database") // Tên của database file
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
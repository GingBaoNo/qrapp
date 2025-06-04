package com.example.qrapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // Import này để tải Bitmap từ file
import android.graphics.drawable.BitmapDrawable; // Giữ lại cho việc lấy Bitmap từ ImageView nếu cần
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log; // Thêm Log để debug
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.qrapp.Entity.QRCodeScan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ScanDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_ID = "extra_scan_id";
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_SCANNED_CONTENT = "extra_scanned_content";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    private static final int PERMISSION_REQUEST_CODE = 100; // Có thể đổi tên thành SAVE_IMAGE_PERMISSION_CODE để rõ ràng hơn

    private ImageView fullQrImage;
    private TextView detailQrContent;
    private TextView detailQrTimestamp;
    private Button btnSaveImage;
    private Button btnDeleteScan;

    private HistoryViewModel historyViewModel;
    private int scanId;
    private String imagePath; // Lưu đường dẫn file nội bộ
    private String scannedContent; // Lưu nội dung quét

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_detail);

        fullQrImage = findViewById(R.id.fullQrImage);
        detailQrContent = findViewById(R.id.detailQrContent);
        detailQrTimestamp = findViewById(R.id.detailQrTimestamp);
        btnSaveImage = findViewById(R.id.btnSaveImage);
        btnDeleteScan = findViewById(R.id.btnDeleteScan);

        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        Intent intent = getIntent();
        if (intent != null) {
            scanId = intent.getIntExtra(EXTRA_SCAN_ID, -1);
            imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);
            scannedContent = intent.getStringExtra(EXTRA_SCANNED_CONTENT); // Lấy nội dung
            long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);

            // --- Cập nhật cách tải ảnh từ đường dẫn nội bộ ---
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    // Tải ảnh từ file nội bộ bằng Glide
                    Glide.with(this)
                            .load(Uri.fromFile(imageFile)) // Tạo URI từ đối tượng File
                            .fitCenter()
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(fullQrImage);
                } else {
                    fullQrImage.setImageResource(R.drawable.ic_launcher_foreground); // Ảnh lỗi nếu file không tồn tại
                    Toast.makeText(this, "Không tìm thấy ảnh gốc.", Toast.LENGTH_SHORT).show();
                    Log.e("ScanDetailActivity", "File ảnh nội bộ không tồn tại: " + imagePath);
                }
            } else {
                fullQrImage.setImageResource(R.drawable.ic_launcher_background); // Ảnh mặc định nếu không có đường dẫn
            }

            detailQrContent.setText("Nội dung: " + scannedContent);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new java.util.Date(timestamp));
            detailQrTimestamp.setText("Quét lúc: " + formattedDate);
        }

        btnSaveImage.setOnClickListener(v -> checkAndRequestPermissions());

        btnDeleteScan.setOnClickListener(v -> {
            if (scanId != -1) {
                // Xóa file ảnh nội bộ trước khi xóa bản ghi DB
                if (imagePath != null && !imagePath.isEmpty()) {
                    File fileToDelete = new File(imagePath);
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            Toast.makeText(ScanDetailActivity.this, "Đã xóa ảnh khỏi bộ nhớ nội bộ.", Toast.LENGTH_SHORT).show();
                            Log.d("ScanDetailActivity", "Ảnh đã được xóa khỏi bộ nhớ nội bộ: " + imagePath);
                        } else {
                            Toast.makeText(ScanDetailActivity.this, "Không thể xóa ảnh từ bộ nhớ nội bộ.", Toast.LENGTH_SHORT).show();
                            Log.e("ScanDetailActivity", "Không thể xóa ảnh từ bộ nhớ nội bộ: " + imagePath);
                        }
                    }
                }

                historyViewModel.deleteScanById(scanId);
                Toast.makeText(ScanDetailActivity.this, "Đã xóa bản quét khỏi lịch sử.", Toast.LENGTH_SHORT).show();
                finish(); // Đóng Activity này sau khi xóa
            } else {
                Toast.makeText(ScanDetailActivity.this, "Không thể xóa. ID không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        // Đối với Android 10 (Q) trở lên, không cần quyền WRITE_EXTERNAL_STORAGE để lưu vào MediaStore
        // Tuy nhiên, nếu bạn muốn lưu vào một thư mục riêng của ứng dụng trên bộ nhớ ngoài (private external storage),
        // bạn vẫn cần quyền nếu target SDK < 29. Nhưng ở đây ta lưu vào MediaStore nên không cần.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery();
        } else {
            // Đối với Android 9 (P) trở xuống, cần quyền WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                saveImageToGallery();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery();
            } else {
                Toast.makeText(this, "Quyền lưu trữ bị từ chối. Không thể lưu ảnh.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery() {
        // Tải Bitmap từ đường dẫn ảnh nội bộ đã lưu trong Room
        Bitmap bitmap = null;
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            } else {
                Log.e("ScanDetailActivity", "File ảnh nội bộ không tồn tại khi cố gắng lưu ra gallery: " + imagePath);
            }
        }

        // Fallback: nếu không tải được từ đường dẫn nội bộ, lấy từ ImageView (có thể là placeholder)
        if (bitmap == null && fullQrImage.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) fullQrImage.getDrawable()).getBitmap();
        }

        if (bitmap == null) {
            Toast.makeText(this, "Không có ảnh hợp lệ để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = "QRScan_Gallery_" + System.currentTimeMillis() + ".jpg"; // Tên file mới cho ảnh lưu ra gallery
        OutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "QRAppScans");
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1); // Đánh dấu là đang chờ xử lý

                Uri imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri savedUri = resolver.insert(imageCollection, contentValues);

                if (savedUri != null) {
                    try (OutputStream os = resolver.openOutputStream(savedUri)) {
                        if (os != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        }
                    }
                    contentValues.clear();
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0); // Bỏ đánh dấu đang chờ xử lý
                    resolver.update(savedUri, contentValues, null, null);
                    Toast.makeText(this, "Ảnh đã được lưu vào thư viện!", Toast.LENGTH_SHORT).show();
                    Log.d("ScanDetailActivity", "Ảnh đã lưu vào thư viện: " + savedUri.toString());
                } else {
                    Toast.makeText(this, "Không thể lưu ảnh vào thư viện.", Toast.LENGTH_SHORT).show();
                    Log.e("ScanDetailActivity", "Không thể chèn URI MediaStore.");
                }
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File qrAppDir = new File(imagesDir, "QRAppScans");
                if (!qrAppDir.exists()) {
                    qrAppDir.mkdirs();
                }
                File image = new File(qrAppDir, filename);
                try (FileOutputStream fosLegacy = new FileOutputStream(image)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fosLegacy);
                }
                // Thông báo MediaScanner để nó thêm ảnh vào thư viện
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(image));
                sendBroadcast(mediaScanIntent);
                Toast.makeText(this, "Ảnh đã được lưu vào thư viện!", Toast.LENGTH_SHORT).show();
                Log.d("ScanDetailActivity", "Ảnh đã lưu vào thư viện: " + image.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi I/O khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ScanDetailActivity", "Lỗi I/O khi lưu ảnh vào gallery: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi không xác định khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ScanDetailActivity", "Lỗi không xác định khi lưu ảnh vào gallery: " + e.getMessage());
        }
    }
}
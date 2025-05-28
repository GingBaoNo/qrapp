package com.example.qrapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView fullQrImage;
    private TextView detailQrContent;
    private TextView detailQrTimestamp;
    private Button btnSaveImage;
    private Button btnDeleteScan;

    private HistoryViewModel historyViewModel;
    private int scanId;
    private String imagePath; // Store imagePath for saving

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

        // Get data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            scanId = intent.getIntExtra(EXTRA_SCAN_ID, -1);
            imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);
            String scannedContent = intent.getStringExtra(EXTRA_SCANNED_CONTENT);
            long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0);

            if (imagePath != null && !imagePath.isEmpty()) {
                Glide.with(this)
                        .load(Uri.parse(imagePath))
                        .fitCenter() // Use fitCenter to show full image
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(fullQrImage);
            } else {
                fullQrImage.setImageResource(R.drawable.ic_launcher_background);
            }

            detailQrContent.setText("Nội dung: " + scannedContent);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = sdf.format(new java.util.Date(timestamp));
            detailQrTimestamp.setText("Quét lúc: " + formattedDate);
        }

        btnSaveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });

        btnDeleteScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanId != -1) {
                    historyViewModel.deleteScanById(scanId);
                    Toast.makeText(ScanDetailActivity.this, "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity after deletion
                } else {
                    Toast.makeText(ScanDetailActivity.this, "Không thể xóa. ID không hợp lệ.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No storage permission needed for Android 10 (Q) and above if saving to MediaStore
            saveImageToGallery();
        } else {
            // For Android 9 (P) and below, request WRITE_EXTERNAL_STORAGE
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
        // Handle cases where no image is loaded
        if (fullQrImage.getDrawable() == null) {
            Toast.makeText(this, "Không có ảnh để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) fullQrImage.getDrawable()).getBitmap();
        String filename = "QRScan_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "QRAppScans");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(imageUri);
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File qrAppDir = new File(imagesDir, "QRAppScans");
                if (!qrAppDir.exists()) {
                    qrAppDir.mkdirs();
                }
                File image = new File(qrAppDir, filename);
                fos = new FileOutputStream(image);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                Toast.makeText(this, "Ảnh đã được lưu vào thư viện!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể mở luồng đầu ra để lưu ảnh.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
package com.example.qrapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.qrapp.Dao.QRCodeScanDao;
import com.example.qrapp.Database.AppDatabase;
import com.example.qrapp.Entity.QRCodeScan;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity2 extends AppCompatActivity {
    private MaterialButton cameraBtn, galleryBtn, scanBtn, historyBtn, openLinkBtn; // Thêm openLinkBtn
    private ImageView imageIv;
    private TextView resultTv;
    private Uri imageUri = null;
    private String scannedContent = null; // Biến để lưu nội dung quét được

    private BarcodeScanner barcodeScanner;
    private String[] storagePermissions;

    private static final int STORAGE_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> galleryLauncher;

    // Room Database instances
    private QRCodeScanDao qrCodeScanDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left+10, systemBars.top+10, systemBars.right+10, systemBars.bottom+10);
            return insets;
        });

        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        scanBtn = findViewById(R.id.scanBtn);
        imageIv = findViewById(R.id.imageIv);
        resultTv = findViewById(R.id.resultTv);
        historyBtn = findViewById(R.id.historyBtn);
        openLinkBtn = findViewById(R.id.openLinkBtn); // Khởi tạo openLinkBtn

        storagePermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        barcodeScanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build());

        // Khởi tạo Room Database
        qrCodeScanDao = AppDatabase.getDatabase(getApplicationContext()).qrCodeScanDao();

        cameraBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                startActivity(new Intent(this, CameraXLivePreviewActivity.class));
            }
        });

        galleryBtn.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                pickImageGallery();
            } else {
                requestStoragePermission();
            }
        });

        scanBtn.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(this, "Chọn ảnh trước đã...", Toast.LENGTH_SHORT).show();
            } else {
                scanImageFromGallery();
            }
        });

        historyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, HistoryActivity.class);
            startActivity(intent);
        });

        openLinkBtn.setOnClickListener(v -> {
            if (scannedContent != null && isValidUrl(scannedContent)) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(scannedContent));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể mở liên kết: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity2", "Error opening URL: " + e.getMessage());
                }
            }
        });


        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            imageUri = data.getData();
                            imageIv.setImageURI(imageUri);
                            resultTv.setText("Ảnh đã được chọn. Nhấn 'Scan QR' để quét.");
                            scannedContent = null; // Đặt lại nội dung quét khi chọn ảnh mới
                            openLinkBtn.setVisibility(View.GONE); // Ẩn nút liên kết khi chọn ảnh mới
                        }
                    } else {
                        Toast.makeText(this, "Đã hủy chọn ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void scanImageFromGallery() {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            barcodeScanner.process(image)
                    .addOnSuccessListener(this::displayResult)
                    .addOnFailureListener(e -> Toast.makeText(this, "Thất bại khi quét ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displayResult(List<Barcode> barcodes) {
        if (barcodes.isEmpty()) {
            resultTv.setText("Không tìm thấy QR Code nào");
            scannedContent = null;
            openLinkBtn.setVisibility(View.GONE); // Ẩn nút nếu không có QR
            Log.d("QR_SCAN", "Không tìm thấy QR Code nào");
            return;
        }

        StringBuilder resultBuilder = new StringBuilder();
        // Lấy nội dung của QR đầu tiên (thường chỉ có 1 QR trong ảnh)
        Barcode firstBarcode = barcodes.get(0);
        scannedContent = firstBarcode.getRawValue();
        resultBuilder.append("QR Code: ").append(scannedContent);

        resultTv.setText(resultBuilder.toString().trim());
        Log.d("QR_SCAN", "Tìm thấy QR: " + scannedContent);

        // Kiểm tra nếu nội dung quét được là một URL
        if (isValidUrl(scannedContent)) {
            openLinkBtn.setVisibility(View.VISIBLE); // Hiển thị nút
        } else {
            openLinkBtn.setVisibility(View.GONE); // Ẩn nút nếu không phải URL
        }

        // Tự động lưu ảnh và nội dung vào DB và bộ nhớ ngay sau khi quét thành công
        if (imageUri != null && scannedContent != null) {
            saveImageAndContentAndToDb();
        }
    }

    // Helper method to check if a string is a valid URL
    private boolean isValidUrl(String url) {
        // Regex để kiểm tra URL, bao gồm http, https, ftp
        // Đảm bảo đủ các ký tự hợp lệ cho URL
        String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }


    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Đảm bảo gọi super

        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageGallery();
            } else {
                Toast.makeText(this, "Quyền truy cập bộ nhớ bị từ chối. Không thể chọn ảnh từ thư viện.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, CameraXLivePreviewActivity.class));
            } else {
                Toast.makeText(this, "Quyền truy cập camera bị từ chối. Không thể bắt đầu xem trước camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void saveImageAndContentAndToDb() {
//        if (imageUri == null || scannedContent == null) {
//            Toast.makeText(this, "Không có ảnh hoặc nội dung để lưu", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        try {
//            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
//            if (bitmap == null) {
//                Toast.makeText(this, "Không thể tải ảnh từ Uri", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            String fileName = "QR_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
//            String savedImagePath = null; // Biến để lưu đường dẫn ảnh đã lưu
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                ContentResolver resolver = getContentResolver();
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
//                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "QRCodeScans");
//                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
//
//                Uri imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
//                Uri savedUri = resolver.insert(imageCollection, contentValues);
//
//                if (savedUri != null) {
//                    try (OutputStream fos = resolver.openOutputStream(savedUri)) {
//                        if (fos != null) {
//                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                            savedImagePath = savedUri.toString(); // Lưu URI của ảnh đã lưu
//                        }
//                    }
//                    contentValues.clear();
//                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
//                    resolver.update(savedUri, contentValues, null, null);
//                }
//            } else {
//                File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QRCodeScans");
//                if (!storageDir.exists()) {
//                    storageDir.mkdirs();
//                }
//                File imageFile = new File(storageDir, fileName);
//
//                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                    savedImagePath = Uri.fromFile(imageFile).toString(); // Lưu đường dẫn file dưới dạng URI String
//                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                    mediaScanIntent.setData(Uri.fromFile(imageFile));
//                    sendBroadcast(mediaScanIntent);
//                }
//            }
//
//            if (savedImagePath != null) {
//                // Lưu vào Room Database
//                QRCodeScan scan = new QRCodeScan(savedImagePath, scannedContent, System.currentTimeMillis());
//                AppDatabase.databaseWriteExecutor.execute(() -> {
//                    qrCodeScanDao.insert(scan);
//                    runOnUiThread(() -> Toast.makeText(MainActivity2.this, "QR Code đã được lưu vào lịch sử!", Toast.LENGTH_SHORT).show());
//                });
//
//                Log.d("QR_SAVE", "Ảnh và nội dung đã lưu: " + fileName + ", Nội dung: " + scannedContent + ", Đường dẫn: " + savedImagePath);
//            } else {
//                Toast.makeText(this, "Lỗi khi lưu ảnh.", Toast.LENGTH_SHORT).show();
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void saveImageAndContentAndToDb() {
        if (imageUri == null || scannedContent == null) {
            Toast.makeText(this, "Không có ảnh hoặc nội dung để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Bước 1: Tải Bitmap từ Uri đã chọn
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            if (bitmap == null) {
                Toast.makeText(this, "Không thể tải ảnh từ Uri đã chọn", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bước 2: Tạo tên file duy nhất
            String fileName = "QR_Internal_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
            String savedImagePath = null; // Biến để lưu đường dẫn ảnh đã lưu

            // Lấy thư mục lưu trữ nội bộ của ứng dụng
            File internalStorageDir = getFilesDir(); // Đây là thư mục /data/data/com.example.qrapp/files

            // Tạo đối tượng File cho ảnh trong thư mục nội bộ
            File imageFile = new File(internalStorageDir, fileName);

            // Ghi Bitmap vào file bằng FileOutputStream
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Nén ảnh với chất lượng 100%
                savedImagePath = imageFile.getAbsolutePath(); // Lấy đường dẫn tuyệt đối của file đã lưu
            }

            if (savedImagePath != null) {
                // Lưu vào Room Database
                QRCodeScan scan = new QRCodeScan(savedImagePath, scannedContent, System.currentTimeMillis());
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    qrCodeScanDao.insert(scan);
                    runOnUiThread(() ->
                            // Thay đổi thông báo Toast để làm rõ ảnh được lưu nội bộ
                            Toast.makeText(MainActivity2.this, "QR Code đã được lưu vào lịch sử nội bộ!", Toast.LENGTH_SHORT).show()
                    );
                });

                // Thay đổi thông báo Log để làm rõ ảnh được lưu nội bộ
                Log.d("QR_SAVE", "Ảnh và nội dung đã lưu vào lịch sử nội bộ: " + fileName + ", Nội dung: " + scannedContent + ", Đường dẫn: " + savedImagePath);
            } else {
                Toast.makeText(this, "Lỗi khi lưu ảnh vào bộ nhớ nội bộ.", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi I/O khi lưu ảnh nội bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi không xác định khi lưu ảnh nội bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
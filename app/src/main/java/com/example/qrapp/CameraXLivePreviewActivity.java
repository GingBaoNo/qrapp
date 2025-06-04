package com.example.qrapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.qrapp.Dao.QRCodeScanDao;
import com.example.qrapp.Database.AppDatabase;
import com.example.qrapp.Entity.QRCodeScan;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXLivePreviewActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private QRCodeScanDao qrCodeScanDao; // Thêm DAO để lưu vào Room
    private boolean isSaving = false; // Biến cờ để tránh lưu trùng lặp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewView = new PreviewView(this);
        setContentView(previewView);

        scanner = BarcodeScanning.getClient();
        qrCodeScanDao = AppDatabase.getDatabase(getApplicationContext()).qrCodeScanDao(); // Khởi tạo DAO

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis use case for barcode scanning
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Chỉ giữ khung hình mới nhất
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
                    @SuppressLint("UnsafeOptInUsageError") // Bỏ qua cảnh báo về ImageProxy.getImage()
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (imageProxy == null || imageProxy.getImage() == null) {
                            imageProxy.close();
                            return;
                        }

                        // Lấy InputImage từ ImageProxy
                        InputImage inputImage = InputImage.fromMediaImage(
                                imageProxy.getImage(),
                                imageProxy.getImageInfo().getRotationDegrees()
                        );

                        // Xử lý barcode
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    // Kiểm tra xem đã tìm thấy barcode và chưa trong quá trình lưu
                                    if (barcodes.size() > 0 && !isSaving) {
                                        isSaving = true; // Đặt cờ đang lưu
                                        // Xử lý barcode và lưu ảnh
                                        processBarcodesAndSave(barcodes.get(0), imageProxy);
                                    } else {
                                        imageProxy.close(); // Đóng imageProxy nếu không xử lý
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("BarcodeScanner", "Barcode scanning failed", e);
                                    imageProxy.close(); // Đóng imageProxy khi thất bại
                                });
                        // Không đóng imageProxy ở đây, đóng trong addOnCompleteListener hoặc addOnFailureListener
                        // để đảm bảo nó chỉ đóng sau khi ML Kit đã xử lý xong.
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA; // Chọn camera sau

                cameraProvider.unbindAll(); // Ngắt kết nối tất cả các use case trước đó

                // Gắn các use case vào lifecycle của Activity
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi khởi động camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this)); // Chạy listener trên luồng chính
    }

    /**
     * Xử lý barcode và lưu ảnh cùng nội dung vào lịch sử.
     * @param barcode Barcode đầu tiên được phát hiện.
     * @param imageProxy Khung hình ảnh từ CameraX.
     */
    private void processBarcodesAndSave(Barcode barcode, ImageProxy imageProxy) {
        String scannedContent = barcode.getRawValue(); // Lấy nội dung thô của QR code

        // Chuyển đổi ImageProxy thành Bitmap
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        imageProxy.close(); // Đóng ImageProxy ngay sau khi chuyển đổi

        if (bitmap != null) {
            saveBitmapAndContentToHistory(bitmap, scannedContent, barcode);
        } else {
            Toast.makeText(this, "Không thể chuyển đổi ảnh từ CameraX", Toast.LENGTH_SHORT).show();
            isSaving = false; // Reset cờ nếu không thể chuyển đổi
        }
    }

    /**
     * Chuyển đổi ImageProxy sang Bitmap.
     * Lưu ý: Phương pháp này có thể không tối ưu cho hiệu suất cao,
     * nhưng phù hợp cho mục đích chụp ảnh đơn lẻ.
     */
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        if (imageProxy.getFormat() == ImageFormat.YUV_420_888) {
            ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                    imageProxy.getWidth(), imageProxy.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } else {
            // Đối với các định dạng khác, bạn có thể cần xử lý riêng
            // Ví dụ, nếu định dạng là RGBA_8888 (đã hiếm), bạn có thể dùng Bitmap.copyPixelsFromBuffer
            return null; // Hoặc ném ngoại lệ
        }
    }


    /**
     * Lưu Bitmap và nội dung quét được vào bộ nhớ và Room Database.
     * @param bitmap Ảnh Bitmap của QR code.
     * @param scannedContent Nội dung text của QR code.
     * @param barcode Đối tượng Barcode gốc để xử lý các loại khác.
     */
    private void saveBitmapAndContentToHistory(Bitmap bitmap, String scannedContent, Barcode barcode) {
        String fileName = "QR_Camera_Internal_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
        String savedImagePath = null;

        try {
            // Lấy thư mục lưu trữ nội bộ của ứng dụng (private to the app)
            File internalStorageDir = getFilesDir(); // Trả về /data/data/com.example.qrapp/files
            File imageFile = new File(internalStorageDir, fileName);

            // Ghi Bitmap vào file bằng FileOutputStream
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos); // Nén ảnh với chất lượng 90%
                savedImagePath = imageFile.getAbsolutePath(); // Lấy đường dẫn tuyệt đối của file
            }

            if (savedImagePath != null) {
                // Lưu vào Room Database
                QRCodeScan scan = new QRCodeScan(savedImagePath, scannedContent, System.currentTimeMillis());
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    qrCodeScanDao.insert(scan);
                    runOnUiThread(() -> {
                        Toast.makeText(CameraXLivePreviewActivity.this, "QR Code đã được lưu vào lịch sử nội bộ!", Toast.LENGTH_SHORT).show();
                        handleBarcodeAction(barcode); // Xử lý hành động sau khi lưu
                    });
                });
                Log.d("QR_SAVE", "Ảnh và nội dung từ Camera đã lưu vào hệ thống nội bộ: " + fileName + ", Nội dung: " + scannedContent + ", Đường dẫn: " + savedImagePath);
            } else {
                Toast.makeText(this, "Lỗi khi lưu ảnh vào bộ nhớ nội bộ.", Toast.LENGTH_SHORT).show();
                isSaving = false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi I/O khi lưu ảnh nội bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("QR_SAVE", "IO Error saving image internally: " + e.getMessage());
            isSaving = false;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi không xác định khi lưu ảnh nội bộ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("QR_SAVE", "Unknown Error saving image internally: " + e.getMessage());
            isSaving = false;
        }
    }


    /**
     * Xử lý các hành động dựa trên loại barcode.
     * Gọi sau khi đã lưu vào lịch sử.
     * @param barcode Barcode đã được quét.
     */
    private void handleBarcodeAction(Barcode barcode) {
        if (barcode == null) return;

        int valueType = barcode.getValueType();

        switch (valueType) {
            case Barcode.TYPE_URL:
                String url = barcode.getUrl().getUrl();
                if (url != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(this, "URL không hợp lệ: " + barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                }
                break;

            case Barcode.TYPE_TEXT:
                String text = barcode.getRawValue();
                Toast.makeText(this, "Đã quét văn bản: " + text, Toast.LENGTH_LONG).show();
                // Có thể sao chép vào clipboard hoặc hiển thị hộp thoại
                break;

            case Barcode.TYPE_PHONE:
                String phone = barcode.getPhone().getNumber();
                if (phone != null) {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(dialIntent);
                } else {
                    Toast.makeText(this, "Số điện thoại không hợp lệ: " + barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                }
                break;

            case Barcode.TYPE_EMAIL:
                String email = barcode.getEmail().getAddress();
                if (email != null) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                    startActivity(emailIntent);
                } else {
                    Toast.makeText(this, "Email không hợp lệ: " + barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                }
                break;

            case Barcode.TYPE_CALENDAR_EVENT:
            case Barcode.TYPE_CONTACT_INFO:
            case Barcode.TYPE_GEO:
            case Barcode.TYPE_ISBN:
            case Barcode.TYPE_PRODUCT:
            case Barcode.TYPE_SMS:
            case Barcode.TYPE_WIFI:
                // Các loại khác có thể xử lý tùy chỉnh hoặc hiển thị thông báo
                Toast.makeText(this, "Đã quét: " + barcode.getRawValue() + " (Loại: " + valueType + ")", Toast.LENGTH_LONG).show();
                break;

            default:
                Toast.makeText(this, "Đã quét: " + barcode.getRawValue() + " (Loại không xác định)", Toast.LENGTH_LONG).show();
                break;
        }

        // Đóng Activity sau khi xử lý xong (tùy chọn, có thể trở về MainActivity2)
        finish();
    }
}
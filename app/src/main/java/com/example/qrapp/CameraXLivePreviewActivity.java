package com.example.qrapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraXLivePreviewActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewView = new PreviewView(this);
        setContentView(previewView);

        scanner = BarcodeScanning.getClient();

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
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (imageProxy == null || imageProxy.getImage() == null) {
                            imageProxy.close();
                            return;
                        }

                        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    processBarcodes(barcodes);
                                })
                                .addOnFailureListener(e -> Log.e("BarcodeScanner", "Barcode scanning failed", e))
                                .addOnCompleteListener(task -> imageProxy.close());
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processBarcodes(List<Barcode> barcodes) {
        if (barcodes.size() > 0) {
            Barcode barcode = barcodes.get(0);
            int valueType = barcode.getValueType();

            switch (valueType) {
                case Barcode.TYPE_URL:
                    String url = barcode.getUrl().getUrl();
                    // Mở URL bằng trình duyệt
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                    break;

                case Barcode.TYPE_TEXT:
                    String text = barcode.getRawValue();
                    Toast.makeText(this, "Text: " + text, Toast.LENGTH_SHORT).show();
                    break;

                case Barcode.TYPE_PHONE:
                    String phone = barcode.getPhone().getNumber();
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                    startActivity(dialIntent);
                    break;

                case Barcode.TYPE_EMAIL:
                    String email = barcode.getEmail().getAddress();
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
                    startActivity(emailIntent);
                    break;

                default:
                    // Trường hợp không xác định
                    Toast.makeText(this, "Scanned: " + barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

}
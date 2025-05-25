package com.example.qrapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity3 extends AppCompatActivity {

    private TextView qrCodeTV;
    private ImageView qrCodeIV;
    private TextInputEditText dataEdt;
    private Button generateQRBtn;
    private Button saveQRBtn;
    private QRGEncoder qrgEncoder;
    private Bitmap bitmap;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main3);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qrCodeTV = findViewById(R.id.idTVGenerateQr);
        qrCodeIV = findViewById(R.id.idIVQRCode);
        dataEdt = findViewById(R.id.idEdtData);
        generateQRBtn = findViewById(R.id.idBtnGenerateQR);
        saveQRBtn = findViewById(R.id.idBtnSaveQR);

        generateQRBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = dataEdt.getText().toString().trim();
                if (data.isEmpty()) {
                    Toast.makeText(MainActivity3.this, "Please enter some data to generate QR Code.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Lấy kích thước màn hình để tạo QR Code vừa vặn
                WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                Display display = manager.getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                int width = point.x;
                int height = point.y;
                int dimen = (width < height ? width : height) * 3 / 4;

                // Tạo QR code
                qrgEncoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, dimen);
                bitmap = qrgEncoder.getBitmap();

                qrCodeTV.setVisibility(View.GONE);
                qrCodeIV.setImageBitmap(bitmap);

                Toast.makeText(MainActivity3.this, "QR Code generated! You can save it now.", Toast.LENGTH_SHORT).show();
            }
        });

        saveQRBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap == null) {
                    Toast.makeText(MainActivity3.this, "Please generate a QR Code first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveQRCodeToGallery(bitmap, "QR_" + System.currentTimeMillis());
            }
        });
    }

    private void saveQRCodeToGallery(Bitmap bitmap, String fileName) {
        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".png");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyQRCodes");
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    if (fos != null) {
                        fos.close();
                    }
                    Toast.makeText(this, "QR Code saved to gallery", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android dưới 10
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/MyQRCodes";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File image = new File(dir, fileName + ".png");
                fos = new FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                // Cập nhật MediaStore
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(image);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "QR Code saved to gallery", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

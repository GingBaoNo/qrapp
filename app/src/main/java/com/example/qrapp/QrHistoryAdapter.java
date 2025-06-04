package com.example.qrapp;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.qrapp.Entity.QRCodeScan;

import java.io.File; // Import File để làm việc với đường dẫn ảnh nội bộ
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class QrHistoryAdapter extends RecyclerView.Adapter<QrHistoryAdapter.QrHistoryViewHolder> {

    private List<QRCodeScan> scanList;

    public QrHistoryAdapter(List<QRCodeScan> scanList) {
        this.scanList = scanList;
    }

    public void setScanList(List<QRCodeScan> newScanList) {
        this.scanList = newScanList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QrHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_qr_history, parent, false);
        return new QrHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QrHistoryViewHolder holder, int position) {
        QRCodeScan currentScan = scanList.get(position);

        String imagePath = currentScan.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            // Tạo Uri từ đường dẫn file nội bộ
            Uri imageUri = Uri.fromFile(new File(imagePath));
            Glide.with(holder.itemView.getContext())
                    .load(imageUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background) // Ảnh placeholder khi đang tải
                    .error(R.drawable.ic_launcher_foreground) // Ảnh lỗi nếu tải thất bại
                    .into(holder.qrImage);
        } else {
            holder.qrImage.setImageResource(R.drawable.ic_launcher_background); // Đặt ảnh mặc định nếu không có đường dẫn
        }

        holder.qrContent.setText("Nội dung: " + currentScan.getScannedContent());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new java.util.Date(currentScan.getTimestamp()));
        holder.qrTimestamp.setText("Quét lúc: " + formattedDate);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ScanDetailActivity.class);
            intent.putExtra(ScanDetailActivity.EXTRA_SCAN_ID, currentScan.getId());
            intent.putExtra(ScanDetailActivity.EXTRA_IMAGE_PATH, currentScan.getImagePath());
            intent.putExtra(ScanDetailActivity.EXTRA_SCANNED_CONTENT, currentScan.getScannedContent());
            intent.putExtra(ScanDetailActivity.EXTRA_TIMESTAMP, currentScan.getTimestamp());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return scanList.size();
    }

    public static class QrHistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView qrImage;
        TextView qrContent;
        TextView qrTimestamp;

        public QrHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            qrImage = itemView.findViewById(R.id.itemQrImage);
            qrContent = itemView.findViewById(R.id.itemQrContent);
            qrTimestamp = itemView.findViewById(R.id.itemQrTimestamp);
        }
    }
}
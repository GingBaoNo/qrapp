package com.example.qrapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.qrapp.Dao.QRCodeScanDao;
import com.example.qrapp.Database.AppDatabase;
import com.example.qrapp.Entity.QRCodeScan;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private QRCodeScanDao qrCodeScanDao;
    private LiveData<List<QRCodeScan>> allScans;

    public HistoryViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        qrCodeScanDao = db.qrCodeScanDao();
        allScans = qrCodeScanDao.getAllScans();
    }

    public LiveData<List<QRCodeScan>> getAllScans() {
        return allScans;
    }

    public void insert(QRCodeScan qrCodeScan) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            qrCodeScanDao.insert(qrCodeScan);
        });
    }

    public void deleteAll() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            qrCodeScanDao.deleteAllScans();
        });
    }

    // New method to delete a scan by ID
    public void deleteScanById(int scanId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            qrCodeScanDao.deleteScanById(scanId);
        });
    }
}
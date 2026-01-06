package com.app.ectrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class StockCheckActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String pharmacyId;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchScanner();
                } else {
                    Toast.makeText(this, "Kamera izni verilmediği için işlem yapılamıyor.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    checkDrugExists(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_check);

        pharmacyId = getIntent().getStringExtra("pharmacyId");
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnStartScan).setOnClickListener(v -> checkPermissionAndScan());
    }

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("İlaç Barkodunu Tara");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class); // Dik ekran için
        barcodeLauncher.launch(options);
    }

    private void checkDrugExists(String barcode) {
        db.collection("drugs").document(barcode).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        showAddStockDialog(barcode, name);
                    } else {
                        Toast.makeText(this, "İlaç sistemde yok, kayıt ekranına gidiliyor...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, NewDrugActivity.class);
                        intent.putExtra("barcode", barcode);
                        intent.putExtra("pharmacyId", pharmacyId);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Veritabanı hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showAddStockDialog(String barcode, String drugName) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this, R.style.CustomDialog);

        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_stock, null);
        builder.setView(dialogView);

        EditText input = dialogView.findViewById(R.id.inputQty);
        TextView txtDrugName = dialogView.findViewById(R.id.txtDrugName);
        txtDrugName.setText(drugName);

        builder.setPositiveButton("Ekle", null);
        builder.setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String val = input.getText().toString().trim();
            if (val.isEmpty()) {
                input.setError("Bu alan boş bırakılamaz");
                return;
            }

            int qty = Integer.parseInt(val);
            if (qty <= 0) {
                input.setError("0'dan büyük bir değer giriniz");
                return;
            }

            addStockToPharmacy(barcode, qty);
            dialog.dismiss();
        });
    }


    private void addStockToPharmacy(String barcode, int qty) {
        db.collection("pharmacies").document(pharmacyId)
                .collection("drugs").document(barcode)
                .update("stok", FieldValue.increment(qty))
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Stok başarıyla güncellendi!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("stok", qty);
                    db.collection("pharmacies").document(pharmacyId)
                            .collection("drugs").document(barcode)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "İlk stok kaydı oluşturuldu!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }
}
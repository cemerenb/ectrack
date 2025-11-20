package com.app.ectrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class PrescribeActivity extends AppCompatActivity {

    private EditText etPatientId, etDrugBarcode, etPackageCount, etDailyUse, etNotes;
    private CheckBox cbMorning, cbNoon, cbEvening, cbOther;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;
    private boolean isScanningPatient = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchScannerInternal();
                } else {
                    Toast.makeText(this, "Kamera izni gerekli!", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    if (isScanningPatient) {
                        etPatientId.setText(result.getContents());
                    } else {
                        etDrugBarcode.setText(result.getContents());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescribe);

        pharmacyId = getIntent().getStringExtra("pharmacyId");
        if (pharmacyId == null) {
            Toast.makeText(this, "Hata: Eczane ID bulunamadı", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        setupListeners();
    }

    private void initViews() {
        etPatientId = findViewById(R.id.etPatientId);
        etDrugBarcode = findViewById(R.id.etDrugBarcode);
        etPackageCount = findViewById(R.id.etPackageCount);
        etDailyUse = findViewById(R.id.etDailyUse);
        etNotes = findViewById(R.id.etNotes);
        cbMorning = findViewById(R.id.cbMorning);
        cbNoon = findViewById(R.id.cbNoon);
        cbEvening = findViewById(R.id.cbEvening);
        cbOther = findViewById(R.id.cbOther);
    }

    private void setupListeners() {
        findViewById(R.id.btnScanPatient).setOnClickListener(v -> startScan(true));
        findViewById(R.id.btnScanDrug).setOnClickListener(v -> startScan(false));
        findViewById(R.id.btnSavePrescription).setOnClickListener(v -> savePrescription());
    }

    private void startScan(boolean forPatient) {
        isScanningPatient = forPatient;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScannerInternal();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScannerInternal() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(isScanningPatient ? "Hasta QR Kodunu Okutun" : "İlaç Barkodunu Okutun");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barcodeLauncher.launch(options);
    }

    private void savePrescription() {
        String patientId = etPatientId.getText().toString().trim();
        String barcode = etDrugBarcode.getText().toString().trim();
        String pkgStr = etPackageCount.getText().toString().trim();

        if (patientId.isEmpty() || barcode.isEmpty() || pkgStr.isEmpty()) {
            Toast.makeText(this, "Lütfen gerekli alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        int packageQty;
        try {
            packageQty = Integer.parseInt(pkgStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Paket adedi geçersiz.", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference stockRef = db.collection("pharmacies").document(pharmacyId).collection("drugs").document(barcode);

        int finalPackageQty = packageQty;
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(stockRef);

            long currentStock = 0;
            if (snapshot.exists() && snapshot.contains("stok")) {
                Double val = snapshot.getDouble("stok"); // Bazen long bazen double dönebilir, güvenli yöntem
                if (val != null) currentStock = val.longValue();
            }

            if (currentStock < finalPackageQty) {
                throw new FirebaseFirestoreException("Yetersiz Stok! Mevcut Stok: " + currentStock, FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(stockRef, "stok", currentStock - finalPackageQty);

            DocumentReference newReceteRef = db.collection("receteler").document();
            Map<String, Object> recete = new HashMap<>();
            recete.put("pharmacyId", pharmacyId);
            recete.put("hastaId", patientId);
            recete.put("eczaciId", auth.getUid());
            recete.put("tarih", FieldValue.serverTimestamp());

            Map<String, Object> ilac = new HashMap<>();
            ilac.put("barkod", barcode);
            ilac.put("paketAdet", finalPackageQty);
            ilac.put("gunlukKullanim", etDailyUse.getText().toString());
            ilac.put("sabah", cbMorning.isChecked());
            ilac.put("ogle", cbNoon.isChecked());
            ilac.put("aksam", cbEvening.isChecked());
            ilac.put("diger", cbOther.isChecked());
            ilac.put("not", etNotes.getText().toString());

            recete.put("ilacDetay", ilac);
            transaction.set(newReceteRef, recete);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Reçete başarıyla kaydedildi.", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
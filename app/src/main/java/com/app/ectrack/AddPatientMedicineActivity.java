package com.app.ectrack;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddPatientMedicineActivity extends AppCompatActivity {

    private TextInputLayout layoutName, layoutDosage, layoutFrequency, layoutDuration;
    private TextInputEditText inputName, inputDosage, inputFrequency, inputDuration;
    private MaterialButton btnSave, btnScan;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient_medicine);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();

        toolbar.setNavigationOnClickListener(v -> finish());
        setupBarcodeScanner();
    }

    private void setupBarcodeScanner() {
        btnScan.setOnClickListener(v -> {
            com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions options = new com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                    .build();

            com.google.mlkit.vision.codescanner.GmsBarcodeScanner scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning
                    .getClient(this, options);

            scanner.startScan()
                    .addOnSuccessListener(barcode -> {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null) {
                            inputName.setText(rawValue);
                            Toast.makeText(this, "Barkod Okundu", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Tarama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void initViews() {
        layoutName = findViewById(R.id.layoutName);
        layoutDosage = findViewById(R.id.layoutDosage);
        layoutFrequency = findViewById(R.id.layoutFrequency);
        layoutDuration = findViewById(R.id.layoutDuration);
        inputName = findViewById(R.id.inputName);
        inputDosage = findViewById(R.id.inputDosage);
        inputFrequency = findViewById(R.id.inputFrequency);
        inputDuration = findViewById(R.id.inputDuration);
        btnSave = findViewById(R.id.btnSave);
        btnScan = findViewById(R.id.btnScan);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveMedicine();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (Objects.requireNonNull(inputName.getText()).toString().trim().isEmpty()) {
            layoutName.setError("İlaç adı gerekli");
            isValid = false;
        } else {
            layoutName.setError(null);
        }

        if (Objects.requireNonNull(inputDuration.getText()).toString().trim().isEmpty()) {
            layoutDuration.setError("Süre gerekli");
            isValid = false;
        } else {
            layoutDuration.setError(null);
        }

        return isValid;
    }

    private void saveMedicine() {
        btnSave.setEnabled(false);
        String userId = auth.getCurrentUser().getUid();

        String name = inputName.getText().toString().trim();
        String dosage = inputDosage.getText().toString().trim();
        String frequency = inputFrequency.getText().toString().trim();
        int duration = Integer.parseInt(inputDuration.getText().toString().trim());

        Map<String, Object> medicine = new HashMap<>();
        medicine.put("name", name);
        medicine.put("dosage", dosage);
        medicine.put("frequency", frequency);
        medicine.put("durationDays", duration);
        medicine.put("startDate", com.google.firebase.Timestamp.now());
        medicine.put("status", "active");

        db.collection("users").document(userId)
                .collection("my_medicines").add(medicine)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Takip başlatıldı", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


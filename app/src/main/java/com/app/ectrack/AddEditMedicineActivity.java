package com.app.ectrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AddEditMedicineActivity extends AppCompatActivity {

    private TextInputLayout layoutName, layoutBarcode, layoutStock, layoutPrice, layoutPieces, layoutType;
    private TextInputEditText inputName, inputBarcode, inputDescription, inputStock, inputPrice, inputExpiry,
            inputPieces;
    private android.widget.AutoCompleteTextView inputType;
    private MaterialButton btnSave, btnDelete, btnScan;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private String pharmacyId;
    private String medicineId;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        initViews();

        pharmacyId = getIntent().getStringExtra("pharmacyId");
        medicineId = getIntent().getStringExtra("medicineId");

        if (medicineId != null) {
            setupEditMode();
        } else {
            toolbar.setTitle("İlaç Ekle");
        }

        setupClickListeners();
        setupBarcodeScanner();
    }

    private void setupClickListeners() {
        inputExpiry.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                saveMedicine();
            }
        });

        btnDelete.setOnClickListener(v -> deleteMedicine());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        inputExpiry.setText(sdf.format(calendar.getTime()));
    }

    private void initViews() {
        layoutName = findViewById(R.id.layoutName);
        layoutBarcode = findViewById(R.id.layoutBarcode);
        layoutStock = findViewById(R.id.layoutStock);
        layoutPrice = findViewById(R.id.layoutPrice);
        layoutPieces = findViewById(R.id.layoutPieces);
        layoutType = findViewById(R.id.layoutType);

        inputName = findViewById(R.id.inputName);
        inputBarcode = findViewById(R.id.inputBarcode);
        inputDescription = findViewById(R.id.inputDescription);
        inputStock = findViewById(R.id.inputStock);
        inputPrice = findViewById(R.id.inputPrice);
        inputExpiry = findViewById(R.id.inputExpiry);
        inputPieces = findViewById(R.id.inputPieces);
        inputType = findViewById(R.id.inputType);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnScan = findViewById(R.id.btnScan);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupEditMode() {
        toolbar.setTitle("İlaç Düzenle");
        btnDelete.setVisibility(android.view.View.VISIBLE);

        inputName.setText(getIntent().getStringExtra("name"));
        inputBarcode.setText(getIntent().getStringExtra("barcode"));
        inputDescription.setText(getIntent().getStringExtra("description"));
        inputStock.setText(String.valueOf(getIntent().getIntExtra("stock", 0)));
        inputPrice.setText(String.valueOf(getIntent().getDoubleExtra("price", 0.0)));
        inputPieces.setText(String.valueOf(getIntent().getIntExtra("piecesPerBox", 0)));
        inputType.setText(getIntent().getStringExtra("medicineType"), false);

        inputBarcode.setEnabled(false);
        btnScan.setVisibility(android.view.View.GONE); // Hide scanner
        inputStock.setEnabled(false);

        long expiryMillis = getIntent().getLongExtra("expiryDate", 0);
        if (expiryMillis > 0) {
            calendar.setTimeInMillis(expiryMillis);
            updateDateLabel();
        }
    }


    private boolean validateInputs() {
        boolean isValid = true;

        if (Objects.requireNonNull(inputName.getText()).toString().trim().isEmpty()) {
            layoutName.setError("İsim gerekli");
            isValid = false;
        } else {
            layoutName.setError(null);
        }

        if (Objects.requireNonNull(inputStock.getText()).toString().trim().isEmpty()) {
            layoutStock.setError("Stok gerekli");
            isValid = false;
        } else {
            layoutStock.setError(null);
        }

        if (Objects.requireNonNull(inputPrice.getText()).toString().trim().isEmpty()) {
            layoutPrice.setError("Fiyat gerekli");
            isValid = false;
        } else {
            layoutPrice.setError(null);
        }

        if (Objects.requireNonNull(inputPieces.getText()).toString().trim().isEmpty()) {
            layoutPieces.setError("Kutu içi adet gerekli");
            isValid = false;
        } else {
            layoutPieces.setError(null);
        }

        if (inputType.getText().toString().trim().isEmpty()) {
            layoutType.setError("İlaç tipi gerekli");
            isValid = false;
        } else {
            layoutType.setError(null);
        }

        return isValid;
    }

    private void saveMedicine() {
        btnSave.setEnabled(false);

        String name = inputName.getText().toString().trim();
        String barcode = inputBarcode.getText().toString().trim();
        String description = inputDescription.getText().toString().trim();
        int stock = Integer.parseInt(inputStock.getText().toString().trim());
        double price = Double.parseDouble(inputPrice.getText().toString().trim());
        int piecesPerBox = Integer.parseInt(inputPieces.getText().toString().trim());
        String medicineType = inputType.getText().toString().trim();

        Timestamp expiryDate = inputExpiry.getText().toString().isEmpty() ? null : new Timestamp(calendar.getTime());

        Map<String, Object> medicine = new HashMap<>();
        medicine.put("name", name);
        medicine.put("barcode", barcode);
        medicine.put("description", description);
        medicine.put("stock", stock);
        medicine.put("price", price);
        medicine.put("piecesPerBox", piecesPerBox);
        medicine.put("medicineType", medicineType);
        medicine.put("expiryDate", expiryDate);

        if (medicineId == null) {

            db.collection("pharmacies").document(pharmacyId)
                    .collection("medicines").add(medicine)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "İlaç eklendi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {

            db.collection("pharmacies").document(pharmacyId)
                    .collection("medicines").document(medicineId)
                    .update(medicine)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "İlaç güncellendi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteMedicine() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("İlacı Sil")
                .setMessage("Bu ilacı silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    db.collection("pharmacies").document(pharmacyId)
                            .collection("medicines").document(medicineId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "İlaç silindi", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hayır", null)
                .show();
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
                            inputBarcode.setText(rawValue);
                            Toast.makeText(this, "Barkod Okundu", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(this, "Tarama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}


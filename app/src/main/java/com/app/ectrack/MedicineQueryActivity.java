package com.app.ectrack;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

public class MedicineQueryActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout layoutBarcode;
    private TextInputEditText inputBarcode;
    private MaterialButton btnQuery, btnScan;
    private MaterialCardView resultCard;
    private TextView textName, textStock, textPrice, textDescription;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_query);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadPharmacyId();

        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutBarcode = findViewById(R.id.layoutBarcode);
        inputBarcode = findViewById(R.id.inputBarcode);
        btnQuery = findViewById(R.id.btnQuery);
        btnScan = findViewById(R.id.btnScan);
        resultCard = findViewById(R.id.resultCard);
        textName = findViewById(R.id.textName);
        textStock = findViewById(R.id.textStock);
        textPrice = findViewById(R.id.textPrice);
        textDescription = findViewById(R.id.textDescription);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadPharmacyId() {
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            pharmacyId = document.getString("pharmacyId");
                        }
                    });
        }
    }

    private void setupListeners() {
        btnScan.setOnClickListener(v -> startScanning());

        btnQuery.setOnClickListener(v -> {
            String barcode = inputBarcode.getText().toString().trim();
            if (!barcode.isEmpty()) {
                queryMedicine(barcode);
            } else {
                layoutBarcode.setError("Barkod giriniz");
            }
        });
    }

    private void startScanning() {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        inputBarcode.setText(rawValue);
                        queryMedicine(rawValue); // Optional: Auto-query on scan
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Tarama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void queryMedicine(String barcode) {
        if (pharmacyId == null) {
            Toast.makeText(this, "Eczane bilgisi yüklenemedi", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);

        db.collection("pharmacies").document(pharmacyId)
                .collection("medicines")
                .whereEqualTo("barcode", barcode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Medicine medicine = doc.toObject(Medicine.class);
                        if (medicine != null) {
                            showResult(medicine);
                        }
                    } else {
                        Toast.makeText(this, "İlaç bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showResult(Medicine medicine) {
        resultCard.setVisibility(View.VISIBLE);
        textName.setText(medicine.getName());
        textStock.setText("Stok: " + medicine.getStock());
        textPrice.setText("Fiyat: " + medicine.getPrice() + " TL");
        textDescription.setText("Açıklama: " + (medicine.getDescription() != null ? medicine.getDescription() : "-"));

        resultCard.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(MedicineQueryActivity.this,
                    MedicineDetailsActivity.class);
            intent.putExtra("name", medicine.getName());
            intent.putExtra("barcode", medicine.getBarcode());
            intent.putExtra("stock", medicine.getStock());
            intent.putExtra("price", medicine.getPrice());
            intent.putExtra("medicineType", medicine.getMedicineType());
            intent.putExtra("piecesPerBox", medicine.getPiecesPerBox());
            intent.putExtra("description", medicine.getDescription());
            if (medicine.getExpiryDate() != null) {
                intent.putExtra("expiryDate", medicine.getExpiryDate().getSeconds() * 1000);
            }
            startActivity(intent);
        });
    }
}


package com.app.ectrack;

import android.os.Bundle;
import android.view.View;
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

import java.util.Objects;

public class AddStockActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout layoutBarcode, layoutQuantity;
    private TextInputEditText inputBarcode, inputQuantity;
    private MaterialButton btnSearch, btnAddStock, btnScan;
    private MaterialCardView resultCard;
    private TextView textName, textCurrentStock;

    private FirebaseFirestore db;
    private String pharmacyId;
    private Medicine selectedMedicine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);

        db = FirebaseFirestore.getInstance();
        pharmacyId = getIntent().getStringExtra("pharmacyId");

        initViews();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutBarcode = findViewById(R.id.layoutBarcode);
        layoutQuantity = findViewById(R.id.layoutQuantity);
        inputBarcode = findViewById(R.id.inputBarcode);
        inputQuantity = findViewById(R.id.inputQuantity);
        btnSearch = findViewById(R.id.btnSearch);
        btnAddStock = findViewById(R.id.btnAddStock);
        btnScan = findViewById(R.id.btnScan);
        resultCard = findViewById(R.id.resultCard);
        textName = findViewById(R.id.textName);
        textCurrentStock = findViewById(R.id.textCurrentStock);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnScan.setOnClickListener(v -> startScanning());

        btnSearch.setOnClickListener(v -> {
            String barcode = Objects.requireNonNull(inputBarcode.getText()).toString().trim();
            if (!barcode.isEmpty()) {
                findMedicine(barcode);
            } else {
                layoutBarcode.setError("Barkod giriniz");
            }
        });

        btnAddStock.setOnClickListener(v -> addStock());
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
                        findMedicine(rawValue);
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Tarama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void findMedicine(String barcode) {
        if (pharmacyId == null)
            return;

        layoutBarcode.setError(null);
        resultCard.setVisibility(View.GONE);

        db.collection("pharmacies").document(pharmacyId)
                .collection("medicines")
                .whereEqualTo("barcode", barcode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        selectedMedicine = doc.toObject(Medicine.class);
                        if (selectedMedicine != null) {
                            selectedMedicine.setId(doc.getId());
                            showMedicineDetails();
                        }
                    } else {
                        selectedMedicine = null;
                        Toast.makeText(this, "İlaç bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showMedicineDetails() {
        resultCard.setVisibility(View.VISIBLE);
        textName.setText(selectedMedicine.getName());
        textCurrentStock.setText("Mevcut Stok: " + selectedMedicine.getStock());
        inputQuantity.setText("");
        layoutQuantity.setError(null);
    }

    private void addStock() {
        if (selectedMedicine == null)
            return;

        String quantityStr = Objects.requireNonNull(inputQuantity.getText()).toString().trim();
        if (quantityStr.isEmpty()) {
            layoutQuantity.setError("Adet giriniz");
            return;
        }

        int quantityToAdd;
        try {
            quantityToAdd = Integer.parseInt(quantityStr);
            if (quantityToAdd <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            layoutQuantity.setError("Geçerli bir sayı giriniz");
            return;
        }

        int newStock = selectedMedicine.getStock() + quantityToAdd;

        btnAddStock.setEnabled(false);
        db.collection("pharmacies").document(pharmacyId)
                .collection("medicines").document(selectedMedicine.getId())
                .update("stock", newStock)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Stok Eklendi. Yeni Stok: " + newStock, Toast.LENGTH_LONG).show();

                    selectedMedicine.setStock(newStock);
                    showMedicineDetails();
                    btnAddStock.setEnabled(true);
                    inputQuantity.setText("");
                })
                .addOnFailureListener(e -> {
                    btnAddStock.setEnabled(true);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


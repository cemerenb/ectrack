package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class DrugQueryActivity extends AppCompatActivity {
    private TextInputEditText barcodeInput;
    private MaterialButton btnSearch, btnScanBarcode, btnAddToStock;
    private MaterialCardView drugInfoCard, stockCard, errorCard;
    private TextView drugNameText, drugDescriptionText, drugPriceText, drugBarcodeText;
    private TextView stockStatusText, stockQuantityText, errorMessageText;
    private LinearLayout stockDetailsLayout;
    private FirebaseFirestore db;
    private String currentPharmacyId;
    private String currentDrugBarcode;
    private boolean drugExistsInPharmacy = false;
    private int currentStockQuantity = 0;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    barcodeInput.setText(result.getContents());
                    searchDrug(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drug_query);

        db = FirebaseFirestore.getInstance();
        currentPharmacyId = getIntent().getStringExtra("pharmacyId");

        if (currentPharmacyId == null) {
            Toast.makeText(this, "Eczane bilgisi alınamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        barcodeInput = findViewById(R.id.barcodeInput);
        btnSearch = findViewById(R.id.btnSearch);
        btnScanBarcode = findViewById(R.id.btnScanBarcode);
        btnAddToStock = findViewById(R.id.btnAddToStock);
        drugInfoCard = findViewById(R.id.drugInfoCard);
        stockCard = findViewById(R.id.stockCard);
        errorCard = findViewById(R.id.errorCard);
        drugNameText = findViewById(R.id.drugNameText);
        drugDescriptionText = findViewById(R.id.drugDescriptionText);
        drugPriceText = findViewById(R.id.drugPriceText);
        drugBarcodeText = findViewById(R.id.drugBarcodeText);
        stockStatusText = findViewById(R.id.stockStatusText);
        stockQuantityText = findViewById(R.id.stockQuantityText);
        stockDetailsLayout = findViewById(R.id.stockDetailsLayout);
        errorMessageText = findViewById(R.id.errorMessageText);
    }

    private void setupClickListeners() {
        btnSearch.setOnClickListener(v -> {
            String barcode = barcodeInput.getText().toString().trim();
            if (barcode.isEmpty()) {
                showError("Lütfen barkod numarası girin");
                return;
            }
            searchDrug(barcode);
        });

        btnScanBarcode.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Barkodu tarayın");
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureAct.class);
            barcodeLauncher.launch(options);
        });

        btnAddToStock.setOnClickListener(v -> {
            if (currentDrugBarcode != null) {
                Intent intent = new Intent(DrugQueryActivity.this, StockCheckActivity.class);
                intent.putExtra("pharmacyId", currentPharmacyId);
                intent.putExtra("barcode", currentDrugBarcode);
                startActivity(intent);
                finish();
            }
        });
    }

    private void searchDrug(String barcode) {
        hideAllCards();
        currentDrugBarcode = barcode;

        db.collection("drugs")
                .whereEqualTo("barcode", barcode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("DrugQuery", "Ana drugs sorgusu - Bulunan kayıt: " + querySnapshot.size());

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        android.util.Log.d("DrugQuery", "İlaç bulundu: " + document.getString("name"));
                        displayDrugInfo(document);
                        checkPharmacyStock(barcode);
                    } else {
                        android.util.Log.w("DrugQuery", "Bu barkoda ait ilaç bulunamadı: " + barcode);
                        showError("Bu barkoda ait ilaç bulunamadı");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DrugQuery", "İlaç arama hatası: " + e.getMessage(), e);
                    showError("İlaç sorgulanırken hata oluştu: " + e.getMessage());
                });
    }

    private void displayDrugInfo(DocumentSnapshot document) {
        String name = document.getString("name");
        String description = document.getString("description");
        String price = document.getString("price");
        String barcode = document.getString("barcode");

        drugNameText.setText(name != null ? name : "-");
        drugDescriptionText.setText(description != null ? description : "-");
        drugPriceText.setText(price != null ? price + " ₺" : "-");
        drugBarcodeText.setText(barcode != null ? barcode : "-");

        drugInfoCard.setVisibility(View.VISIBLE);
    }

    private void checkPharmacyStock(String barcode) {
        db.collection("pharmacies")
                .document(currentPharmacyId)
                .collection("drugs")
                .document(barcode)  // Barkod doküman ID'si olarak kullanılıyor
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    android.util.Log.d("DrugQuery", "Sorgu başarılı - Doküman var mı: " + documentSnapshot.exists());

                    stockCard.setVisibility(View.VISIBLE);

                    if (documentSnapshot.exists()) {
                        drugExistsInPharmacy = true;
                        Long stockQuantity = documentSnapshot.getLong("stok");
                        currentStockQuantity = stockQuantity != null ? stockQuantity.intValue() : 0;
                        displayStockInfo(true, currentStockQuantity);
                    } else {
                        drugExistsInPharmacy = false;
                        currentStockQuantity = 0;
                        displayStockInfo(false, 0);
                    }
                })
                .addOnFailureListener(e -> {
                    stockCard.setVisibility(View.VISIBLE);
                    stockStatusText.setText("Stok bilgisi alınamadı");
                    stockDetailsLayout.setVisibility(View.GONE);
                });
    }

    private void displayStockInfo(boolean inStock, int quantity) {

        if (inStock) {
            if (quantity > 0) {
                stockStatusText.setText("Bu ilaç eczanenizde mevcut");
                stockStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                stockQuantityText.setText("Stok Miktarı: " + quantity + " adet");
                btnAddToStock.setText("Stok Güncelle");
            } else {
                stockStatusText.setText("Stok tükenmiş");
                stockStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                stockQuantityText.setText("Stok Miktarı: 0 adet");
                btnAddToStock.setText("Stoğa Ekle");
            }
        } else {
            stockStatusText.setText("Bu ilaç eczanenizde bulunmuyor");
            stockStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            stockQuantityText.setText("");
            btnAddToStock.setText("Eczaneye Ekle");
        }

        stockDetailsLayout.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        hideAllCards();
        errorMessageText.setText(message);
        errorCard.setVisibility(View.VISIBLE);
    }

    private void hideAllCards() {
        drugInfoCard.setVisibility(View.GONE);
        stockCard.setVisibility(View.GONE);
        errorCard.setVisibility(View.GONE);
    }
}
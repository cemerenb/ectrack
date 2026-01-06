package com.app.ectrack;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PrescribeMedicineActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout layoutIdentityNumber, layoutMedicineBarcode, layoutDosage, layoutDuration,
            layoutUsageInstructions, layoutBoxQuantity;
    private TextInputEditText inputIdentityNumber, inputMedicineBarcode, inputDosage, inputDuration,
            inputUsageInstructions, inputBoxQuantity;
    private TextView textMedicineName, textMedicineStock, textMedicinePrice, textMedicineDetails,
            textPatientName, textPatientAge;
    private com.google.android.material.card.MaterialCardView cardMedicineDetails, cardPatientDetails;
    private MaterialButton btnPrescribe, btnScanIdentity, btnScanMedicine, btnQueryIdentity;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;
    private Medicine selectedMedicine;
    private String selectedPatientId;
    private String selectedPatientName;
    private boolean isPatientVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescribe_medicine);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        loadPharmacyId();

        toolbar.setNavigationOnClickListener(v -> finish());

        setupBarcodeScanner();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutIdentityNumber = findViewById(R.id.layoutIdentityNumber);
        layoutMedicineBarcode = findViewById(R.id.layoutMedicineBarcode);
        layoutDosage = findViewById(R.id.layoutDosage);
        layoutDuration = findViewById(R.id.layoutDuration);
        layoutUsageInstructions = findViewById(R.id.layoutUsageInstructions);
        layoutBoxQuantity = findViewById(R.id.layoutBoxQuantity);
        inputIdentityNumber = findViewById(R.id.inputIdentityNumber);
        inputMedicineBarcode = findViewById(R.id.inputMedicineBarcode);
        inputDosage = findViewById(R.id.inputDosage);
        inputDuration = findViewById(R.id.inputDuration);
        inputUsageInstructions = findViewById(R.id.inputUsageInstructions);
        inputBoxQuantity = findViewById(R.id.inputBoxQuantity);

        cardMedicineDetails = findViewById(R.id.cardMedicineDetails);
        cardPatientDetails = findViewById(R.id.cardPatientDetails);
        textMedicineName = findViewById(R.id.textMedicineName);
        textMedicineStock = findViewById(R.id.textMedicineStock);
        textMedicinePrice = findViewById(R.id.textMedicinePrice);
        textMedicineDetails = findViewById(R.id.textMedicineDetails);
        textPatientName = findViewById(R.id.textPatientName);
        textPatientAge = findViewById(R.id.textPatientAge);

        btnPrescribe = findViewById(R.id.btnPrescribe);
        btnScanIdentity = findViewById(R.id.btnScanIdentity);
        btnScanMedicine = findViewById(R.id.btnScanMedicine);
        btnQueryIdentity = findViewById(R.id.btnQueryIdentity);
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

    private void setupBarcodeScanner() {

        btnScanMedicine.setOnClickListener(v -> scanBarcode(inputMedicineBarcode));

        btnScanIdentity.setOnClickListener(v -> scanBarcode(inputIdentityNumber));
    }

    private void scanBarcode(TextInputEditText targetInput) {
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        targetInput.setText(rawValue);
                        if (targetInput == inputIdentityNumber && rawValue.length() == 11) {
                            findPatient(rawValue);
                        }
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Tarama hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupListeners() {
        btnQueryIdentity.setOnClickListener(v -> {
            String tc = Objects.requireNonNull(inputIdentityNumber.getText()).toString().trim();
            if (tc.length() == 11) {
                findPatient(tc);
            } else {
                setErrorPreservingIcon(layoutIdentityNumber, "T.C. No 11 haneli olmalıdır");
            }
        });

        inputIdentityNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPatientVerified = false;
                cardPatientDetails.setVisibility(android.view.View.GONE);
                selectedPatientId = null;
                selectedPatientName = null;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        inputMedicineBarcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 3) {
                    findMedicine(s.toString());
                } else {
                    selectedMedicine = null;
                    cardMedicineDetails.setVisibility(android.view.View.GONE);
                }
            }
        });

        btnPrescribe.setOnClickListener(v -> {
            if (validateInputs()) {
                prescribeMedicine();
            }
        });
    }

    private void findPatient(String tc) {
        btnQueryIdentity.setEnabled(false);
        db.collection("users")
                .whereEqualTo("identityNumber", tc)
                .whereEqualTo("userType", "patient")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    btnQueryIdentity.setEnabled(true);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments()
                                .get(0);
                        selectedPatientId = doc.getId();
                        selectedPatientName = doc.getString("fullName");
                        Long age = doc.getLong("age");

                        isPatientVerified = true;
                        cardPatientDetails.setVisibility(android.view.View.VISIBLE);
                        textPatientName.setText(
                                "Hasta Adı: " + (selectedPatientName != null ? selectedPatientName : "İsimsiz"));
                        textPatientAge.setText("Yaş: " + (age != null ? age : "-"));
                        clearErrorPreservingIcon(layoutIdentityNumber);
                    } else {
                        isPatientVerified = false;
                        cardPatientDetails.setVisibility(android.view.View.GONE);
                        setErrorPreservingIcon(layoutIdentityNumber, "Hasta bulunamadı");
                    }
                })
                .addOnFailureListener(e -> {
                    btnQueryIdentity.setEnabled(true);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void findMedicine(String barcode) {
        if (pharmacyId == null)
            return;

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

                            clearErrorPreservingIcon(layoutMedicineBarcode);

                            cardMedicineDetails.setVisibility(android.view.View.VISIBLE);
                            textMedicineName.setText(selectedMedicine.getName());
                            textMedicineStock.setText("Stok: " + selectedMedicine.getStock());
                            textMedicinePrice.setText("Fiyat: " + selectedMedicine.getPrice() + " TL");
                            textMedicineDetails.setText("Tip: " + selectedMedicine.getMedicineType() + " | Kutu: "
                                    + selectedMedicine.getPiecesPerBox());

                            if (selectedMedicine.getStock() <= 3) {
                                textMedicineStock.setTextColor(android.graphics.Color.RED);
                            } else {
                                textMedicineStock.setTextColor(android.graphics.Color.parseColor("#388E3C")); // Green
                            }
                        }
                    } else {
                        selectedMedicine = null;
                        cardMedicineDetails.setVisibility(android.view.View.GONE);

                        Toast.makeText(this, "Bu barkoda ait ilaç bulunamadı", Toast.LENGTH_SHORT).show();
                        setErrorPreservingIcon(layoutMedicineBarcode, "İlaç bulunamadı");
                    }
                });
    }

    private void setErrorPreservingIcon(com.google.android.material.textfield.TextInputLayout layout, String error) {
        layout.setError(error);

        if (layout == layoutMedicineBarcode || layout == layoutIdentityNumber) {
            layout.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
        }
    }

    private void clearErrorPreservingIcon(com.google.android.material.textfield.TextInputLayout layout) {
        layout.setError(null);

        if (layout == layoutMedicineBarcode || layout == layoutIdentityNumber) {
            layout.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
        }
    }

    private boolean validateInputs() {
        if (Objects.requireNonNull(inputIdentityNumber.getText()).toString().trim().isEmpty()) {
            setErrorPreservingIcon(layoutIdentityNumber, "Gerekli");
            return false;
        }
        if (!isPatientVerified) {
            setErrorPreservingIcon(layoutIdentityNumber, "Lütfen hastayı sorgulayın");
            return false;
        }
        if (selectedMedicine == null) {
            setErrorPreservingIcon(layoutMedicineBarcode, "Geçerli bir ilaç girin");
            return false;
        }
        if (Objects.requireNonNull(inputDosage.getText()).toString().trim().isEmpty()) {
            setErrorPreservingIcon(layoutDosage, "Doz bilgisi gerekli");
            return false;
        }
        if (Objects.requireNonNull(inputDuration.getText()).toString().trim().isEmpty()) {
            setErrorPreservingIcon(layoutDuration, "Süre bilgisi gerekli");
            return false;
        }
        if (selectedMedicine.getStock() <= 0) {
            Toast.makeText(this, "Stok yetersiz!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void prescribeMedicine() {
        btnPrescribe.setEnabled(false);
        String dosage = inputDosage.getText().toString().trim();
        String duration = inputDuration.getText().toString().trim();
        String instructions = Objects.requireNonNull(inputUsageInstructions.getText()).toString().trim();

        int boxQuantity = 1;
        String boxQtyStr = Objects.requireNonNull(inputBoxQuantity.getText()).toString().trim();
        if (!boxQtyStr.isEmpty()) {
            try {
                boxQuantity = Integer.parseInt(boxQtyStr);
            } catch (NumberFormatException e) {
                boxQuantity = 1;
            }
        }

        final int finalBoxQuantity = boxQuantity;

        Map<String, Object> patientMedicine = new HashMap<>();
        patientMedicine.put("medicineId", selectedMedicine.getId());
        patientMedicine.put("name", selectedMedicine.getName());
        patientMedicine.put("dosage", dosage);
        patientMedicine.put("duration", duration);
        patientMedicine.put("usageInstructions", instructions);
        patientMedicine.put("prescribedDate", com.google.firebase.Timestamp.now());
        patientMedicine.put("pharmacyId", pharmacyId);

        Map<String, Object> prescriptionRecord = new HashMap<>();
        prescriptionRecord.put("patientId", selectedPatientId);
        prescriptionRecord.put("patientName", selectedPatientName != null ? selectedPatientName : "İsimsiz");
        prescriptionRecord.put("medicineId", selectedMedicine.getId());
        prescriptionRecord.put("medicineName", selectedMedicine.getName());
        prescriptionRecord.put("dosage", dosage);
        prescriptionRecord.put("duration", duration);
        prescriptionRecord.put("boxQuantity", finalBoxQuantity);
        prescriptionRecord.put("usageInstructions", instructions);
        prescriptionRecord.put("prescribedDate", com.google.firebase.Timestamp.now());
        prescriptionRecord.put("employeeId", auth.getCurrentUser().getUid());

        db.collection("users").document(selectedPatientId)
                .collection("my_medicines").add(patientMedicine)
                .addOnSuccessListener(docRef -> {

                    db.collection("pharmacies").document(pharmacyId)
                            .collection("prescriptions").add(prescriptionRecord)
                            .addOnSuccessListener(prescRef -> {

                                decrementStock();
                            })
                            .addOnFailureListener(e -> {

                                decrementStock();
                            });
                })
                .addOnFailureListener(e -> {
                    btnPrescribe.setEnabled(true);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void decrementStock() {

        int boxQuantity = 1;
        String boxQtyStr = Objects.requireNonNull(inputBoxQuantity.getText()).toString().trim();
        if (!boxQtyStr.isEmpty()) {
            try {
                boxQuantity = Integer.parseInt(boxQtyStr);
            } catch (NumberFormatException e) {
                boxQuantity = 1;
            }
        }

        int newStock = selectedMedicine.getStock() - boxQuantity;

        db.collection("pharmacies").document(pharmacyId)
                .collection("medicines").document(selectedMedicine.getId())
                .update("stock", newStock)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "İlaç başarıyla yazıldı!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this, "Stok güncellenemedi, ancak hastaya eklendi.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}


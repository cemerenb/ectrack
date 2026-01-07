package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PharmacyDashboardActivity extends AppCompatActivity {
    private TextView pharmacyNameText;
    private MaterialButton logoutButton;
    private com.google.android.material.card.MaterialCardView cardMedicines;
    private com.google.android.material.card.MaterialCardView cardEmployees;
    private com.google.android.material.card.MaterialCardView cardSettings;
    private com.google.android.material.card.MaterialCardView cardQueryMedicine;
    private com.google.android.material.card.MaterialCardView cardPrescribeMedicine;
    private com.google.android.material.card.MaterialCardView cardAddStock;
    private com.google.android.material.card.MaterialCardView cardPrescriptionHistory;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentPharmacyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        loadUserInfo();
        setupClickListeners();
    }

    private void initViews() {
        pharmacyNameText = findViewById(R.id.pharmacyNameText);
        logoutButton = findViewById(R.id.logoutButton);
        cardMedicines = findViewById(R.id.cardMedicines);
        cardEmployees = findViewById(R.id.cardEmployees);
        cardSettings = findViewById(R.id.cardSettings);
        cardQueryMedicine = findViewById(R.id.cardQueryMedicine);
        cardPrescribeMedicine = findViewById(R.id.cardPrescribeMedicine);
        cardAddStock = findViewById(R.id.cardAddStock);
        cardPrescriptionHistory = findViewById(R.id.cardPrescriptionHistory);
    }

    private void loadUserInfo() {
        if (auth.getCurrentUser() == null)
            return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("userType");
                        String role = document.getString("role");
                        currentPharmacyId = document.getString("pharmacyId");

                        if (currentPharmacyId != null) {
                            loadPharmacyInfo(currentPharmacyId);
                        }

                        if ("owner".equals(role)) {
                            cardEmployees.setVisibility(android.view.View.VISIBLE);
                            cardSettings.setVisibility(android.view.View.VISIBLE);
                        } else {

                            cardEmployees.setVisibility(android.view.View.GONE);
                            cardSettings.setVisibility(android.view.View.GONE);
                        }
                    }
                })
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Kullanıcı bilgisi alınamadı", Toast.LENGTH_SHORT).show());
    }

    private void loadPharmacyInfo(String pharmacyId) {
        db.collection("pharmacies").document(pharmacyId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name != null)
                            pharmacyNameText.setText(name);
                    }
                });
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(PharmacyDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        cardMedicines.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, MedicineListActivity.class);
            startActivity(intent);
        });

        cardEmployees.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, EmployeeListActivity.class);
            intent.putExtra("pharmacyId", currentPharmacyId);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, PharmacyEditActivity.class);
            startActivity(intent);
        });

        cardQueryMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, MedicineQueryActivity.class);
            startActivity(intent);
        });

        cardPrescribeMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, PrescribeMedicineActivity.class);
            startActivity(intent);
        });

        cardAddStock.setOnClickListener(v -> {
            if (currentPharmacyId != null) {
                Intent intent = new Intent(PharmacyDashboardActivity.this, AddStockActivity.class);
                intent.putExtra("pharmacyId", currentPharmacyId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Eczane bilgisi yüklenmedi", Toast.LENGTH_SHORT).show();
            }
        });

        cardPrescriptionHistory.setOnClickListener(v -> {
            Intent intent = new Intent(PharmacyDashboardActivity.this, PrescriptionHistoryActivity.class);
            startActivity(intent);
        });
    }
}

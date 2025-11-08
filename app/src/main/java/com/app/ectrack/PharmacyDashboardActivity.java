package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PharmacyDashboardActivity extends AppCompatActivity {

    private TextView pharmacyNameText;
    private TextView userRoleText;
    private MaterialButton logoutButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

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
        userRoleText = findViewById(R.id.userRoleText);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void loadUserInfo() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("userType");
                        String pharmacyId = document.getString("pharmacyId");

                        if (userType != null) {
                            String roleText = userType.equals("pharmacy_owner") ? "Eczane Sahibi" : "Eczane Çalışanı";
                            userRoleText.setText(roleText);
                        }

                        if (pharmacyId != null) {
                            loadPharmacyInfo(pharmacyId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Bilgiler yüklenemedi", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPharmacyInfo(String pharmacyId) {
        db.collection("pharmacies").document(pharmacyId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name != null) {
                            pharmacyNameText.setText(name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eczane bilgileri yüklenemedi", Toast.LENGTH_SHORT).show();
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
    }
}
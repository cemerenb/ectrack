package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView welcomeText;
    private Button logoutButton;
    private com.google.android.material.card.MaterialCardView cardMyMedicines;
    private com.google.android.material.card.MaterialCardView cardFindPharmacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        initViews();
        displayUserInfo();

        setupClickListeners();
    }

    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);
        cardMyMedicines = findViewById(R.id.cardMyMedicines);
        cardFindPharmacy = findViewById(R.id.cardFindPharmacy);
    }

    private void displayUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fullName = document.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                welcomeText.setText("Hoş geldiniz, " + fullName);
                            } else {
                                welcomeText.setText("Hoş geldiniz, " + user.getEmail());
                            }
                        }
                    })
                    .addOnFailureListener(e -> {

                        welcomeText.setText("Hoş geldiniz, " + user.getEmail());
                    });
        }
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> logout());

        cardMyMedicines.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PatientMedicineListActivity.class);
            startActivity(intent);
        });

        cardFindPharmacy.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PharmacySearchActivity.class);
            startActivity(intent);
        });
    }

    private void logout() {
        auth.signOut();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

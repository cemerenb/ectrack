package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

public class RoleSelectionActivity extends AppCompatActivity {

    private CardView pharmacyOwnerCard;
    private CardView pharmacyEmployeeCard;
    private CardView patientCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        pharmacyOwnerCard = findViewById(R.id.pharmacyOwnerCard);
        pharmacyEmployeeCard = findViewById(R.id.pharmacyEmployeeCard);
        patientCard = findViewById(R.id.patientCard);
    }

    private void setupClickListeners() {
        pharmacyOwnerCard.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, PharmacyOwnerSetupActivity.class);
            startActivity(intent);
        });

        pharmacyEmployeeCard.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, PharmacyEmployeeSetupActivity.class);
            startActivity(intent);
        });

        patientCard.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, PatientSetupActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
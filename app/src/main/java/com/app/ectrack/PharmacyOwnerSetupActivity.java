package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class PharmacyOwnerSetupActivity extends AppCompatActivity {

    private TextInputLayout inviteCodeLayout;
    private TextInputEditText inviteCodeInput;
    private MaterialButton verifyButton;
    private MaterialButton skipButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_owner_setup);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        inviteCodeLayout = findViewById(R.id.inviteCodeLayout);
        inviteCodeInput = findViewById(R.id.inviteCodeInput);
        verifyButton = findViewById(R.id.verifyButton);
        skipButton = findViewById(R.id.skipButton);
    }

    private void setupClickListeners() {
        verifyButton.setOnClickListener(v -> {
            String code = inviteCodeInput.getText().toString().trim();
            validateInviteCode(code);
        });

        skipButton.setOnClickListener(v -> {
            navigateToPharmacyDetails();
        });
    }

    private void validateInviteCode(String code) {
        if (code.isEmpty()) {
            inviteCodeLayout.setError("Davet kodu gerekli");
            return;
        }

        if (code.toLowerCase().startsWith("e")) {
            inviteCodeLayout.setError(null);
            Toast.makeText(this, "Davet kodu geçerli!", Toast.LENGTH_SHORT).show();
            navigateToPharmacyDetails();
        } else {
            inviteCodeLayout.setError("Geçersiz davet kodu");
        }
    }

    private void navigateToPharmacyDetails() {
        Intent intent = new Intent(PharmacyOwnerSetupActivity.this, PharmacyDetailsActivity.class);
        startActivity(intent);
        finish();
    }
}
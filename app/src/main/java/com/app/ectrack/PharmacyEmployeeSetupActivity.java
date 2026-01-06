package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PharmacyEmployeeSetupActivity extends AppCompatActivity {

    private TextInputLayout inviteCodeLayout;
    private TextInputEditText inviteCodeInput;
    private MaterialButton joinButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_employee_setup);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        inviteCodeLayout = findViewById(R.id.inviteCodeLayout);
        inviteCodeInput = findViewById(R.id.inviteCodeInput);
        joinButton = findViewById(R.id.joinButton);
    }

    private void setupClickListeners() {
        joinButton.setOnClickListener(v -> {
            String code = inviteCodeInput.getText().toString().trim();
            validateAndJoinPharmacy(code);
        });
    }

    private void validateAndJoinPharmacy(String inviteCode) {
        if (inviteCode.isEmpty()) {
            inviteCodeLayout.setError("Davet kodu gerekli");
            return;
        }

        inviteCodeLayout.setError(null);
        joinButton.setEnabled(false);
        joinButton.setText("Kontrol ediliyor...");

        db.collection("invitations")
                .whereEqualTo("inviteCode", inviteCode)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot invitation = queryDocumentSnapshots.getDocuments().get(0);
                        String pharmacyId = invitation.getString("pharmacyId");

                        if (pharmacyId != null) {
                            joinPharmacy(pharmacyId, invitation.getId());
                        } else {
                            showError("Geçersiz davet kodu");
                        }
                    } else {
                        showError("Geçersiz veya kullanılmış davet kodu");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Hata: " + e.getMessage());
                });
    }

    private void joinPharmacy(String pharmacyId, String invitationId) {
        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        String name = auth.getCurrentUser().getDisplayName();

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("fullName", name != null ? name : "İsimsiz");
        user.put("userType", "pharmacy_employee");
        user.put("pharmacyId", pharmacyId);
        user.put("role", "employee");
        user.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    addEmployeeToPharmacy(pharmacyId, userId, invitationId);
                })
                .addOnFailureListener(e -> {
                    showError("Hata: " + e.getMessage());
                });
    }

    private void addEmployeeToPharmacy(String pharmacyId, String userId, String invitationId) {
        Map<String, Object> employee = new HashMap<>();
        employee.put("userId", userId);
        employee.put("email", auth.getCurrentUser().getEmail());
        employee.put("fullName",
                auth.getCurrentUser().getDisplayName() != null ? auth.getCurrentUser().getDisplayName() : "İsimsiz");
        employee.put("role", "employee");
        employee.put("joinedAt", com.google.firebase.Timestamp.now());
        employee.put("status", "active");

        db.collection("pharmacies")
                .document(pharmacyId)
                .collection("employees")
                .document(userId)
                .set(employee)
                .addOnSuccessListener(aVoid -> {
                    markInvitationAsUsed(invitationId);
                })
                .addOnFailureListener(e -> {
                    showError("Hata: " + e.getMessage());
                });
    }

    private void markInvitationAsUsed(String invitationId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "used");
        updates.put("usedAt", com.google.firebase.Timestamp.now());
        updates.put("usedBy", auth.getCurrentUser().getUid());

        db.collection("invitations").document(invitationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Eczaneye başarıyla katıldınız!", Toast.LENGTH_SHORT).show();
                    navigateToPharmacyDashboard();
                })
                .addOnFailureListener(e -> {
                    navigateToPharmacyDashboard();
                });
    }

    private void showError(String message) {
        joinButton.setEnabled(true);
        joinButton.setText("Katıl");
        inviteCodeLayout.setError(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToPharmacyDashboard() {
        Intent intent = new Intent(PharmacyEmployeeSetupActivity.this, PharmacyDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PatientSetupActivity extends AppCompatActivity {

    private TextInputLayout identityNumberLayout;
    private TextInputLayout ageLayout;
    private TextInputLayout heightLayout;
    private TextInputLayout weightLayout;
    private TextInputEditText identityNumberInput;
    private TextInputEditText ageInput;
    private TextInputEditText heightInput;
    private TextInputEditText weightInput;
    private MaterialButton completeButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_setup);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        identityNumberLayout = findViewById(R.id.identityNumberLayout);
        ageLayout = findViewById(R.id.ageLayout);
        heightLayout = findViewById(R.id.heightLayout);
        weightLayout = findViewById(R.id.weightLayout);
        identityNumberInput = findViewById(R.id.identityNumberInput);
        ageInput = findViewById(R.id.ageInput);
        heightInput = findViewById(R.id.heightInput);
        weightInput = findViewById(R.id.weightInput);
        completeButton = findViewById(R.id.completeButton);
    }

    private void setupClickListeners() {
        completeButton.setOnClickListener(v -> {
            if (validateInputs()) {
                savePatientInfo();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String identityNumber = identityNumberInput.getText().toString().trim();
        String age = ageInput.getText().toString().trim();
        String height = heightInput.getText().toString().trim();
        String weight = weightInput.getText().toString().trim();

        if (identityNumber.isEmpty()) {
            identityNumberLayout.setError("T.C. Kimlik No gerekli");
            isValid = false;
        } else if (identityNumber.length() != 11) {
            identityNumberLayout.setError("T.C. Kimlik No 11 haneli olmalıdır");
            isValid = false;
        } else {
            identityNumberLayout.setError(null);
        }

        if (age.isEmpty()) {
            ageLayout.setError("Yaş gerekli");
            isValid = false;
        } else {
            try {
                int ageValue = Integer.parseInt(age);
                if (ageValue < 0 || ageValue > 150) {
                    ageLayout.setError("Geçerli bir yaş girin (0-150)");
                    isValid = false;
                } else {
                    ageLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                ageLayout.setError("Geçerli bir sayı girin");
                isValid = false;
            }
        }


        if (height.isEmpty()) {
            heightLayout.setError("Boy gerekli");
            isValid = false;
        } else {
            try {
                int heightValue = Integer.parseInt(height);
                if (heightValue < 50 || heightValue > 250) {
                    heightLayout.setError("Geçerli bir boy girin (50-250 cm)");
                    isValid = false;
                } else {
                    heightLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                heightLayout.setError("Geçerli bir sayı girin");
                isValid = false;
            }
        }

        if (weight.isEmpty()) {
            weightLayout.setError("Kilo gerekli");
            isValid = false;
        } else {
            try {
                double weightValue = Double.parseDouble(weight);
                if (weightValue < 10 || weightValue > 500) {
                    weightLayout.setError("Geçerli bir kilo girin (10-500 kg)");
                    isValid = false;
                } else {
                    weightLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                weightLayout.setError("Geçerli bir sayı girin");
                isValid = false;
            }
        }

        return isValid;
    }

    private void savePatientInfo() {
        completeButton.setEnabled(false);
        completeButton.setText("Kaydediliyor...");

        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        String name = auth.getCurrentUser().getDisplayName();
        String identityNumber = identityNumberInput.getText().toString().trim();
        int age = Integer.parseInt(ageInput.getText().toString().trim());
        int height = Integer.parseInt(heightInput.getText().toString().trim());
        double weight = Double.parseDouble(weightInput.getText().toString().trim());

        double heightInMeters = height / 100.0;
        double bmi = weight / (heightInMeters * heightInMeters);

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("fullName", name != null ? name : "İsimsiz");
        user.put("identityNumber", identityNumber);
        user.put("userType", "patient");
        user.put("age", age);
        user.put("height", height);
        user.put("weight", weight);
        user.put("bmi", Math.round(bmi * 10.0) / 10.0);
        user.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profiliniz başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    completeButton.setEnabled(true);
                    completeButton.setText("Tamamla");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(PatientSetupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

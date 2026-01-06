package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private MaterialButton registerButton;
    private TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        registerButton = findViewById(R.id.registerButton);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (validateInputs(name, email, password, confirmPassword)) {
                registerUser(name, email, password);
            }
        });

        backToLogin.setOnClickListener(v -> finish());
    }

    private boolean validateInputs(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (name.isEmpty()) {
            nameInputLayout.setError("Ad Soyad gerekli");
            isValid = false;
        } else if (name.length() < 3) {
            nameInputLayout.setError("Ad Soyad en az 3 karakter olmalı");
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        if (email.isEmpty()) {
            emailInputLayout.setError("E-posta adresi gerekli");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Geçerli bir e-posta adresi girin");
            isValid = false;
        } else {
            emailInputLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Şifre gerekli");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Şifre en az 6 karakter olmalı");
            isValid = false;
        } else {
            passwordInputLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError("Şifre tekrarı gerekli");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Şifreler eşleşmiyor");
            isValid = false;
        } else {
            confirmPasswordInputLayout.setError(null);
        }

        return isValid;
    }

    private void registerUser(String name, String email, String password) {
        showLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, name);
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = getErrorMessage(task.getException());
                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Kayıt başarılı!",
                                Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Profil güncellenemedi",
                                Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    }
                });
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "Kayıt başarısız";
        }

        String message = exception.getMessage();
        if (message == null) {
            return "Kayıt başarısız";
        }

        if (message.contains("email address is already in use")) {
            return "Bu e-posta adresi zaten kullanılıyor";
        } else if (message.contains("badly formatted")) {
            return "E-posta formatı hatalı";
        } else if (message.contains("weak password")) {
            return "Şifre çok zayıf";
        } else if (message.contains("network error")) {
            return "İnternet bağlantınızı kontrol edin";
        } else {
            return "Kayıt başarısız: " + message;
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            registerButton.setEnabled(false);
            registerButton.setText("Kayıt yapılıyor...");
            nameInput.setEnabled(false);
            emailInput.setEnabled(false);
            passwordInput.setEnabled(false);
            confirmPasswordInput.setEnabled(false);
        } else {
            registerButton.setEnabled(true);
            registerButton.setText("Kayıt Ol");
            nameInput.setEnabled(true);
            emailInput.setEnabled(true);
            passwordInput.setEnabled(true);
            confirmPasswordInput.setEnabled(true);
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

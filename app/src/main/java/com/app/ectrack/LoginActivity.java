package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private MaterialButton loginButton;
    private TextView forgotPassword;
    private TextView registerLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            checkUserProfileAndNavigate();
            return;
        }

        setupClickListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        registerLink = findViewById(R.id.registerLink);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (validateInputs(email, password)) {
                loginUser(email, password);
            }
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

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

        return isValid;
    }

    private void loginUser(String email, String password) {
        showLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserProfileAndNavigate();
                    } else {
                        showLoading(false);
                        String errorMessage = getErrorMessage(task.getException());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserProfileAndNavigate() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    showLoading(false);
                    if (document.exists() && document.contains("userType")) {
                        String userType = document.getString("userType");
                        navigateBasedOnRole(userType);
                    } else {
                        navigateToRoleSelection();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Bir hata oluştu", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateBasedOnRole(String userType) {
        Intent intent;
        switch (userType) {
            case "pharmacy_owner":
            case "pharmacy_employee":
                intent = new Intent(LoginActivity.this, PharmacyDashboardActivity.class);
                break;
            case "patient":
                intent = new Intent(LoginActivity.this, MainActivity.class);
                break;
            default:
                navigateToRoleSelection();
                return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRoleSelection() {
        Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) return "Giriş başarısız";
        String message = exception.getMessage();
        if (message == null) return "Giriş başarısız";

        if (message.contains("badly formatted")) {
            return "E-posta formatı hatalı";
        } else if (message.contains("no user record")) {
            return "Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı";
        } else if (message.contains("password is invalid")) {
            return "Şifre hatalı";
        } else if (message.contains("network error")) {
            return "İnternet bağlantınızı kontrol edin";
        } else {
            return "Giriş başarısız: " + message;
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            loginButton.setEnabled(false);
            loginButton.setText("Giriş yapılıyor...");
        } else {
            loginButton.setEnabled(true);
            loginButton.setText("Giriş Yap");
        }
    }
}
package com.app.ectrack;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputEditText emailInput;
    private TextInputLayout emailInputLayout;
    private MaterialButton resetButton;
    private TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        auth = FirebaseAuth.getInstance();
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.emailInput);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        resetButton = findViewById(R.id.resetButton);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupClickListeners() {
        resetButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (validateEmail(email)) {
                sendPasswordResetEmail(email);
            }
        });


        backToLogin.setOnClickListener(v -> finish());
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailInputLayout.setError("E-posta adresi gerekli");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Geçerli bir e-posta adresi girin");
            return false;
        } else {
            emailInputLayout.setError(null);
            return true;
        }
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        showSuccessDialog(email);
                    } else {
                        String errorMessage = getErrorMessage(task.getException());

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "Şifre sıfırlama e-postası gönderilemedi";
        }

        String message = exception.getMessage();
        if (message == null) {
            return "Şifre sıfırlama e-postası gönderilemedi";
        }

        if (message.contains("no user record")) {
            return "Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı";
        } else if (message.contains("badly formatted")) {
            return "E-posta formatı hatalı";
        } else if (message.contains("network error")) {
            return "İnternet bağlantınızı kontrol edin";
        } else {
            return "Şifre sıfırlama e-postası gönderilemedi: " + message;
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            resetButton.setEnabled(false);
            resetButton.setText("Gönderiliyor...");
            emailInput.setEnabled(false);
        } else {
            resetButton.setEnabled(true);
            resetButton.setText("Şifre Sıfırlama Bağlantısı Gönder");
            emailInput.setEnabled(true);
        }
    }

    private void showSuccessDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("E-posta Gönderildi")
                .setMessage("Şifre sıfırlama bağlantısı " + email + " adresine gönderildi. Lütfen e-postanızı kontrol edin.")
                .setPositiveButton("Tamam", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
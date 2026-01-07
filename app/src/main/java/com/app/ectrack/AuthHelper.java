package com.app.ectrack;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.regex.Pattern;

public class AuthHelper {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isPasswordValid(String password) {
        return password != null && pattern.matcher(password).matches();
    }

    public static String formatFirebaseError(Exception exception) {
        if (exception == null)
            return "Bir hata oluştu";
        String message = exception.getMessage();
        if (message == null)
            return "Bir hata oluştu";

        if (message.contains("email address is already in use")) {
            return "Bu e-posta adresi zaten başka bir hesap tarafından kullanılıyor.";
        } else if (message.contains("badly formatted")) {
            return "Geçersiz e-posta formatı. Lütfen kontrol edin.";
        } else if (message.contains("weak password")) {
            return "Şifre çok zayıf. Lütfen daha güçlü bir şifre belirleyin.";
        } else if (message.contains("no user record")) {
            return "Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı.";
        } else if (message.contains("password is invalid") || message.contains("wrong password")
                || message.contains("supplied auth credential")) {
            return "E-posta veya şifre hatalı veya oturum süresi dolmuş. Lütfen tekrar deneyin.";
        } else if (message.contains("network error") || message.contains("Unable to resolve host")) {
            return "Bağlantı hatası. Lütfen internet bağlantınızı kontrol edin.";
        } else if (message.contains("too many unsuccessful login attempts")) {
            return "Çok fazla hatalı giriş denemesi yapıldı. Lütfen daha sonra tekrar deneyin.";
        } else if (message.contains("user-disabled")) {
            return "Bu kullanıcı hesabı devre dışı bırakılmış.";
        } else {
            return "Hata oluştu: " + message;
        }
    }

    public static void showErrorDialog(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Tamam", null)
                .show();
    }
}

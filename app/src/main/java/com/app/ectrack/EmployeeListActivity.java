package com.app.ectrack;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EmployeeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialButton btnGenerateInvite;
    private MaterialToolbar toolbar;
    private EmployeeAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadPharmacyIdAndEmployees();

        btnGenerateInvite.setOnClickListener(v -> generateInviteCode());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnGenerateInvite = findViewById(R.id.btnGenerateInvite);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupRecyclerView() {
        adapter = new EmployeeAdapter();
        adapter.setOnEmployeeDeleteListener(this::confirmDeleteEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadPharmacyIdAndEmployees() {
        if (auth.getCurrentUser() == null)
            return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        pharmacyId = document.getString("pharmacyId");
                        if (pharmacyId != null) {
                            listenToEmployees();
                        }
                    }
                });
    }

    private void listenToEmployees() {

        db.collection("users")
                .whereEqualTo("pharmacyId", pharmacyId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        List<Map<String, Object>> employees = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {

                                data.put("docId", doc.getId());
                                employees.add(data);
                            }
                        }
                        adapter.setEmployees(employees);
                    }
                });
    }

    private void confirmDeleteEmployee(Map<String, Object> employee) {
        String employeeName = (String) employee.get("email");
        new AlertDialog.Builder(this)
                .setTitle("Çalışanı Çıkar")
                .setMessage(employeeName + " adlı çalışanı çıkarmak istediğinize emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> deleteEmployee(employee))
                .setNegativeButton("İptal", null)
                .show();
    }

    private void deleteEmployee(Map<String, Object> employee) {
        String docId = (String) employee.get("docId");
        if (docId == null)
            return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("pharmacyId", null);
        updates.put("userType", "patient"); // Reset to patient or generic role

        db.collection("users").document(docId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Çalışan çıkarıldı.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void generateInviteCode() {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Map<String, Object> invitation = new HashMap<>();
        invitation.put("code", code); // Keeping 'code' for display logic consistency if needed, but 'inviteCode' is

        invitation.put("inviteCode", code);
        invitation.put("pharmacyId", pharmacyId);
        invitation.put("status", "active");
        invitation.put("createdAt", com.google.firebase.Timestamp.now());
        invitation.put("createdBy", auth.getCurrentUser().getUid());

        db.collection("invitations").add(invitation)
                .addOnSuccessListener(documentReference -> {
                    showInviteDialog(code);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showInviteDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("Davet Kodu")
                .setMessage("Bu kodu çalışanınızla paylaşın: " + code)
                .setPositiveButton("Kopyala", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Invite Code", code);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Kopyalandı", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Kapat", null)
                .show();
    }
}


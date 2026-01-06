package com.app.ectrack;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PrescriptionHistoryActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText inputSearch;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private PrescriptionAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;
    private List<Prescription> allPrescriptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_history);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadPharmacyIdAndPrescriptions();
        setupSearch();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        inputSearch = findViewById(R.id.inputSearch);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        adapter = new PrescriptionAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterPrescriptions(s.toString());
            }
        });
    }

    private void filterPrescriptions(String query) {
        if (query.isEmpty()) {
            adapter.setPrescriptions(allPrescriptions);
            toggleEmptyView(allPrescriptions.isEmpty());
            return;
        }

        List<Prescription> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Prescription p : allPrescriptions) {
            if (p.getPatientName() != null && p.getPatientName().toLowerCase().contains(lowerQuery)) {
                filtered.add(p);
            }
        }

        adapter.setPrescriptions(filtered);
        toggleEmptyView(filtered.isEmpty());
    }

    private void loadPharmacyIdAndPrescriptions() {
        if (auth.getCurrentUser() == null)
            return;

        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        pharmacyId = document.getString("pharmacyId");
                        if (pharmacyId != null) {
                            loadPrescriptions();
                        } else {
                            Toast.makeText(this, "Eczane bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPrescriptions() {
        db.collection("pharmacies").document(pharmacyId)
                .collection("prescriptions")
                .orderBy("prescribedDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Veri yüklenemedi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allPrescriptions.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Prescription prescription = doc.toObject(Prescription.class);
                            if (prescription != null) {
                                prescription.setId(doc.getId());
                                allPrescriptions.add(prescription);
                            }
                        }
                    }

                    adapter.setPrescriptions(allPrescriptions);
                    toggleEmptyView(allPrescriptions.isEmpty());
                });
    }

    private void toggleEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}


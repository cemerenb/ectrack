package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private FloatingActionButton fabAddMedicine;
    private TextView emptyView;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadPharmacyIdAndMedicines();

        fabAddMedicine.setOnClickListener(v -> {
            if (pharmacyId != null) {
                Intent intent = new Intent(MedicineListActivity.this, AddEditMedicineActivity.class);
                intent.putExtra("pharmacyId", pharmacyId);
                intent.putExtra("mode", "define");
                startActivity(intent);
            }
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        fabAddMedicine = findViewById(R.id.fabAddMedicine);
        toolbar = findViewById(R.id.toolbar);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        adapter = new MedicineAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(medicine -> {
            Intent intent = new Intent(MedicineListActivity.this, AddEditMedicineActivity.class);
            intent.putExtra("pharmacyId", pharmacyId);
            intent.putExtra("medicineId", medicine.getId());
            intent.putExtra("name", medicine.getName());
            intent.putExtra("barcode", medicine.getBarcode());
            intent.putExtra("description", medicine.getDescription());
            intent.putExtra("stock", medicine.getStock());
            intent.putExtra("price", medicine.getPrice());
            intent.putExtra("piecesPerBox", medicine.getPiecesPerBox());
            intent.putExtra("medicineType", medicine.getMedicineType());
            if (medicine.getExpiryDate() != null) {
                intent.putExtra("expiryDate", medicine.getExpiryDate().getSeconds() * 1000); // Timestamp to millis
            }
            startActivity(intent);
        });
    }

    private void loadPharmacyIdAndMedicines() {
        if (auth.getCurrentUser() == null)
            return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        pharmacyId = document.getString("pharmacyId");
                        if (pharmacyId != null) {
                            listenToMedicines();
                        } else {
                            Toast.makeText(this, "Eczane kaydı bulunamadı", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void listenToMedicines() {
        db.collection("pharmacies").document(pharmacyId)
                .collection("medicines")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Veri yüklenemedi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Medicine> medicineList = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Medicine medicine = doc.toObject(Medicine.class);
                            if (medicine != null) {
                                medicine.setId(doc.getId());
                                medicineList.add(medicine);
                            }
                        }
                    }
                    adapter.setMedicines(medicineList);
                    toggleEmptyView(medicineList.isEmpty());
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


package com.app.ectrack;

import android.content.Intent;
import android.os.Bundle;
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

public class PatientMedicineListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MaterialToolbar toolbar;
    private android.widget.TextView emptyView;
    private PatientMedicineAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_medicine_list);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadMedicines();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);
        emptyView = findViewById(R.id.emptyView);
    }

    private void setupRecyclerView() {
        adapter = new PatientMedicineAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadMedicines() {
        if (auth.getCurrentUser() == null)
            return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("my_medicines")
                .orderBy("prescribedDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    List<PatientMedicine> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PatientMedicine m = doc.toObject(PatientMedicine.class);
                            if (m != null) {
                                m.setId(doc.getId());
                                list.add(m);
                            }
                        }
                    }
                    adapter.setMedicines(list);
                    toggleEmptyView(list.isEmpty());
                });
    }

    private void toggleEmptyView(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            emptyView.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }
}


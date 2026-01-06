package com.app.ectrack;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class NewDrugActivity extends AppCompatActivity {

    private EditText etName, etPrice, etDesc;
    private String barcode;
    private String pharmacyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_drug);

        barcode = getIntent().getStringExtra("barcode");
        pharmacyId = getIntent().getStringExtra("pharmacyId");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvInfo = findViewById(R.id.tvBarcodeInfo);
        tvInfo.setText("Barkod: " + barcode);

        etName = findViewById(R.id.etDrugName);
        etPrice = findViewById(R.id.etPrice);
        etDesc = findViewById(R.id.etDesc);

        findViewById(R.id.btnSaveNewDrug).setOnClickListener(v -> saveDrug());
    }

    private void saveDrug() {
        String name = etName.getText().toString();
        String price = etPrice.getText().toString();
        String desc = etDesc.getText().toString();

        if(name.isEmpty()) {
            etName.setError("İsim zorunlu");
            return;
        }

        Map<String, Object> drug = new HashMap<>();
        drug.put("barcode", barcode);
        drug.put("name", name);
        drug.put("price", price);
        drug.put("description", desc);

        FirebaseFirestore.getInstance().collection("drugs").document(barcode)
                .set(drug)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "İlaç Kaydedildi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hata oluştu", Toast.LENGTH_SHORT).show());
    }
}
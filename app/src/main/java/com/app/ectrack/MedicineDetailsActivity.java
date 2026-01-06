package com.app.ectrack;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MedicineDetailsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView textMedicineName, textBarcode, textStock, textPrice;
    private TextView textMedicineType, textPiecesPerBox, textExpiryDate, textDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_details);

        initViews();
        toolbar.setNavigationOnClickListener(v -> finish());

        loadMedicineDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textMedicineName = findViewById(R.id.textMedicineName);
        textBarcode = findViewById(R.id.textBarcode);
        textStock = findViewById(R.id.textStock);
        textPrice = findViewById(R.id.textPrice);
        textMedicineType = findViewById(R.id.textMedicineType);
        textPiecesPerBox = findViewById(R.id.textPiecesPerBox);
        textExpiryDate = findViewById(R.id.textExpiryDate);
        textDescription = findViewById(R.id.textDescription);
    }

    private void loadMedicineDetails() {

        String name = getIntent().getStringExtra("name");
        String barcode = getIntent().getStringExtra("barcode");
        int stock = getIntent().getIntExtra("stock", 0);
        double price = getIntent().getDoubleExtra("price", 0.0);
        String medicineType = getIntent().getStringExtra("medicineType");
        int piecesPerBox = getIntent().getIntExtra("piecesPerBox", 0);
        long expiryDateMillis = getIntent().getLongExtra("expiryDate", 0);
        String description = getIntent().getStringExtra("description");

        textMedicineName.setText(name != null ? name : "-");
        textBarcode.setText(barcode != null ? barcode : "-");
        textStock.setText(String.valueOf(stock));
        textPrice.setText(String.format(Locale.getDefault(), "%.2f TL", price));
        textMedicineType.setText(medicineType != null ? medicineType : "-");
        textPiecesPerBox.setText(String.valueOf(piecesPerBox));

        if (expiryDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textExpiryDate.setText(sdf.format(new Date(expiryDateMillis)));
        } else {
            textExpiryDate.setText("-");
        }

        textDescription.setText(description != null && !description.isEmpty() ? description : "Açıklama bulunmuyor");

        if (stock <= 3) {
            textStock.setTextColor(android.graphics.Color.RED);
        } else {
            textStock.setTextColor(android.graphics.Color.parseColor("#388E3C"));
        }
    }
}


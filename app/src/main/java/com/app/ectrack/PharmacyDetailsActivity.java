package com.app.ectrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PharmacyDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextInputLayout pharmacyNameLayout;
    private TextInputLayout pharmacyAddressLayout;
    private TextInputEditText pharmacyNameInput;
    private TextInputEditText pharmacyAddressInput;
    private MaterialButton getCurrentLocationButton;
    private MaterialButton completeButton;

    private GoogleMap map;
    private LatLng selectedLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupClickListeners();
    }

    private void initViews() {
        pharmacyNameLayout = findViewById(R.id.pharmacyNameLayout);
        pharmacyAddressLayout = findViewById(R.id.pharmacyAddressLayout);
        pharmacyNameInput = findViewById(R.id.pharmacyNameInput);
        pharmacyAddressInput = findViewById(R.id.pharmacyAddressInput);
        getCurrentLocationButton = findViewById(R.id.getCurrentLocationButton);
        completeButton = findViewById(R.id.completeButton);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        LatLng turkey = new LatLng(39.9334, 32.8597);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));

        map.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            map.clear();
            map.addMarker(new MarkerOptions().position(latLng).title("Eczane Konumu"));
            getAddressFromLocation(latLng);
        });

        checkLocationPermission();
    }

    private void setupClickListeners() {
        getCurrentLocationButton.setOnClickListener(v -> getCurrentLocation());

        completeButton.setOnClickListener(v -> {
            if (validateInputs()) {
                savePharmacyToFirestore();
            }
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            map.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                }
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        map.clear();
                        map.addMarker(new MarkerOptions()
                                .position(selectedLocation)
                                .title("Eczane Konumu"));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                        getAddressFromLocation(selectedLocation);
                    } else {
                        Toast.makeText(this, "Konum alınamadı, lütfen tekrar deneyin", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                pharmacyAddressInput.setText(addressText);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Adres alınamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String name = pharmacyNameInput.getText().toString().trim();
        String address = pharmacyAddressInput.getText().toString().trim();

        if (name.isEmpty()) {
            pharmacyNameLayout.setError("Eczane adı gerekli");
            isValid = false;
        } else {
            pharmacyNameLayout.setError(null);
        }

        if (address.isEmpty()) {
            pharmacyAddressLayout.setError("Adres gerekli");
            isValid = false;
        } else {
            pharmacyAddressLayout.setError(null);
        }

        if (selectedLocation == null) {
            Toast.makeText(this, "Lütfen haritadan konum seçin", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void savePharmacyToFirestore() {
        completeButton.setEnabled(false);
        completeButton.setText("Kaydediliyor...");

        String pharmacyId = db.collection("pharmacies").document().getId();
        String userId = auth.getCurrentUser().getUid();
        String pharmacyName = pharmacyNameInput.getText().toString().trim();
        String pharmacyAddress = pharmacyAddressInput.getText().toString().trim();

        Map<String, Object> pharmacy = new HashMap<>();
        pharmacy.put("pharmacyId", pharmacyId);
        pharmacy.put("name", pharmacyName);
        pharmacy.put("address", pharmacyAddress);
        pharmacy.put("location", new GeoPoint(selectedLocation.latitude, selectedLocation.longitude));
        pharmacy.put("ownerId", userId);
        pharmacy.put("createdAt", com.google.firebase.Timestamp.now());
        pharmacy.put("status", "active");

        db.collection("pharmacies").document(pharmacyId)
                .set(pharmacy)
                .addOnSuccessListener(aVoid -> {
                    // Kullanıcı bilgilerini güncelle
                    updateUserProfile(pharmacyId);
                })
                .addOnFailureListener(e -> {
                    completeButton.setEnabled(true);
                    completeButton.setText("Tamamla");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserProfile(String pharmacyId) {
        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("userType", "pharmacy_owner");
        user.put("pharmacyId", pharmacyId);
        user.put("role", "owner");
        user.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Eczane başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                    navigateToPharmacyDashboard();
                })
                .addOnFailureListener(e -> {
                    completeButton.setEnabled(true);
                    completeButton.setText("Tamamla");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToPharmacyDashboard() {
        Intent intent = new Intent(PharmacyDetailsActivity.this, PharmacyDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
package com.app.ectrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.Objects;

public class PharmacyDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextInputLayout pharmacyNameLayout;
    private TextInputLayout pharmacyPhoneLayout;
    private TextInputLayout pharmacyAddressLayout;
    private TextInputEditText pharmacyNameInput;
    private TextInputEditText pharmacyPhoneInput;
    private TextInputEditText pharmacyAddressInput;
    private TextInputEditText weekdayInput;
    private TextInputEditText weekendInput;
    private MaterialButton completeButton;
    private ImageView centerMarker;

    private GoogleMap map;
    private LatLng selectedLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final Handler handler = new Handler();
    private Runnable addressUpdateRunnable;
    private boolean isUpdatingAddress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupMap();
        setupClickListeners();
    }

    private void initViews() {
        pharmacyNameLayout = findViewById(R.id.pharmacyNameLayout);
        pharmacyAddressLayout = findViewById(R.id.pharmacyAddressLayout);
        pharmacyPhoneLayout = findViewById(R.id.pharmacyPhoneLayout);
        pharmacyNameInput = findViewById(R.id.pharmacyNameInput);
        pharmacyPhoneInput = findViewById(R.id.pharmacyPhoneInput);
        pharmacyAddressInput = findViewById(R.id.pharmacyAddressInput);
        weekdayInput = findViewById(R.id.weekdayInput);
        weekendInput = findViewById(R.id.weekendInput);
        completeButton = findViewById(R.id.completeButton);
        centerMarker = findViewById(R.id.centerMarker);
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
        selectedLocation = turkey;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(turkey, 6));

        map.setOnCameraIdleListener(() -> {
            selectedLocation = map.getCameraPosition().target;

            if (addressUpdateRunnable != null) {
                handler.removeCallbacks(addressUpdateRunnable);
            }

            addressUpdateRunnable = () -> {
                getAddressFromLocation(selectedLocation);
            };
            handler.postDelayed(addressUpdateRunnable, 500);
        });

        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                centerMarker.animate()
                        .translationY(-50)
                        .setDuration(200)
                        .start();
            }
        });

        checkLocationPermission();
    }

    private void setupClickListeners() {

        completeButton.setOnClickListener(v -> {
            if (validateInputs()) {
                savePharmacyToFirestore();
            }
        });

        weekdayInput.setOnClickListener(v -> showTimeRangePicker(weekdayInput));
        weekendInput.setOnClickListener(v -> showTimeRangePicker(weekendInput));
    }

    private void showTimeRangePicker(TextInputEditText input) {

        android.app.TimePickerDialog openPicker = new android.app.TimePickerDialog(this,
                (view, openHour, openMinute) -> {
                    String openTime = String.format(Locale.getDefault(), "%02d:%02d", openHour, openMinute);

                    android.app.TimePickerDialog closePicker = new android.app.TimePickerDialog(this,
                            (view2, closeHour, closeMinute) -> {
                                String closeTime = String.format(Locale.getDefault(), "%02d:%02d", closeHour,
                                        closeMinute);
                                input.setText(openTime + " - " + closeTime);
                            }, 19, 0, true);

                    closePicker.setTitle("Kapanış Saati");
                    closePicker.show();

                }, 9, 0, true);

        openPicker.setTitle("Açılış Saati");
        openPicker.show();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (map != null && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (map != null && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                }
            }
        }
    }

    private void getAddressFromLocation(LatLng latLng) {
        if (isUpdatingAddress)
            return;

        isUpdatingAddress = true;

        new Thread(() -> {
            try {
                if (!Geocoder.isPresent()) {
                    runOnUiThread(() -> isUpdatingAddress = false);
                    return;
                }

                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String addressText = address.getAddressLine(0);
                        if (addressText != null) {
                            pharmacyAddressInput.setText(addressText);
                        }
                    }
                    isUpdatingAddress = false;
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    e.printStackTrace();
                    isUpdatingAddress = false;
                });
            }
        }).start();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String name = Objects.requireNonNull(pharmacyNameInput.getText()).toString().trim();
        String address = Objects.requireNonNull(pharmacyAddressInput.getText()).toString().trim();

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
        String pharmacyPhone = pharmacyPhoneInput.getText().toString().trim();
        String pharmacyAddress = pharmacyAddressInput.getText().toString().trim();
        String weekday = weekdayInput.getText().toString().trim();
        String weekend = weekendInput.getText().toString().trim();

        Map<String, Object> pharmacy = new HashMap<>();
        pharmacy.put("pharmacyId", pharmacyId);
        pharmacy.put("name", pharmacyName);
        pharmacy.put("phone", pharmacyPhone);
        pharmacy.put("address", pharmacyAddress);

        Map<String, String> workingHours = new HashMap<>();
        workingHours.put("weekday", weekday);
        workingHours.put("weekend", weekend);
        pharmacy.put("workingHours", workingHours);

        pharmacy.put("location", new GeoPoint(selectedLocation.latitude, selectedLocation.longitude));
        pharmacy.put("ownerId", userId);
        pharmacy.put("createdAt", com.google.firebase.Timestamp.now());
        pharmacy.put("status", "active");
        pharmacy.put("drugs", new HashMap<>());

        db.collection("pharmacies").document(pharmacyId)
                .set(pharmacy)
                .addOnSuccessListener(aVoid -> updateUserProfile(pharmacyId))
                .addOnFailureListener(e -> {
                    completeButton.setEnabled(true);
                    completeButton.setText("Tamamla");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserProfile(String pharmacyId) {
        String userId = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();
        String name = auth.getCurrentUser().getDisplayName();

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("email", email);
        user.put("fullName", name != null ? name : "İsimsiz");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (addressUpdateRunnable != null) {
            handler.removeCallbacks(addressUpdateRunnable);
        }
    }
}

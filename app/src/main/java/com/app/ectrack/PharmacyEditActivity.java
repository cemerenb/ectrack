package com.app.ectrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
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
import com.google.android.material.appbar.MaterialToolbar;
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

public class PharmacyEditActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextInputLayout pharmacyNameLayout;
    private TextInputLayout pharmacyAddressLayout;
    private TextInputEditText pharmacyNameInput;
    private TextInputEditText pharmacyPhoneInput;
    private TextInputEditText pharmacyAddressInput;
    private TextInputEditText weekdayInput;
    private TextInputEditText weekendInput;
    private MaterialButton updateButton;
    private ImageView centerMarker;
    private MaterialToolbar toolbar;

    private GoogleMap map;
    private LatLng selectedLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String pharmacyId;

    private final Handler handler = new Handler();
    private Runnable addressUpdateRunnable;
    private boolean isUpdatingAddress = false;
    private boolean initialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_edit);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();

        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserAndPharmacyData();
    }

    private void initViews() {
        pharmacyNameLayout = findViewById(R.id.pharmacyNameLayout);
        pharmacyAddressLayout = findViewById(R.id.pharmacyAddressLayout);
        pharmacyNameInput = findViewById(R.id.pharmacyNameInput);
        pharmacyPhoneInput = findViewById(R.id.pharmacyPhoneInput);
        pharmacyAddressInput = findViewById(R.id.pharmacyAddressInput);
        weekdayInput = findViewById(R.id.weekdayInput);
        weekendInput = findViewById(R.id.weekendInput);
        updateButton = findViewById(R.id.updateButton);
        centerMarker = findViewById(R.id.centerMarker);
        toolbar = findViewById(R.id.toolbar);
    }

    private void loadUserAndPharmacyData() {
        if (auth.getCurrentUser() == null)
            return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        pharmacyId = document.getString("pharmacyId");
                        if (pharmacyId != null) {
                            loadPharmacyDetails(pharmacyId);
                        }
                    }
                });
    }

    private void loadPharmacyDetails(String id) {
        db.collection("pharmacies").document(id).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String phone = document.getString("phone");
                        String address = document.getString("address");
                        GeoPoint location = document.getGeoPoint("location");

                        if (document.contains("workingHours")) {
                            Map<String, String> hours = (Map<String, String>) document.get("workingHours");
                            if (hours != null) {
                                weekdayInput.setText(hours.get("weekday"));
                                weekendInput.setText(hours.get("weekend"));
                            }
                        }

                        pharmacyNameInput.setText(name);
                        pharmacyPhoneInput.setText(phone != null ? phone : "");
                        pharmacyAddressInput.setText(address);

                        if (location != null) {
                            selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            setupMap();
                        }
                    }
                });
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

        if (selectedLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
        }

        map.setOnCameraIdleListener(() -> {
            selectedLocation = map.getCameraPosition().target;

            if (initialLoad) {
                initialLoad = false;
                return; // Skip address update on initial load to keep manual address if needed
            }

            if (addressUpdateRunnable != null) {
                handler.removeCallbacks(addressUpdateRunnable);
            }

            addressUpdateRunnable = () -> getAddressFromLocation(selectedLocation);
            handler.postDelayed(addressUpdateRunnable, 500);
        });

        setupClickListeners();
        checkLocationPermission();
    }

    private void setupClickListeners() {
        updateButton.setOnClickListener(v -> {
            if (validateInputs()) {
                updatePharmacy();
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
            if (map != null) {
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
                runOnUiThread(() -> isUpdatingAddress = false);
            }
        }).start();
    }

    private boolean validateInputs() {
        boolean isValid = true;
        if (Objects.requireNonNull(pharmacyNameInput.getText()).toString().trim().isEmpty()) {
            pharmacyNameLayout.setError("Eczane adı gerekli");
            isValid = false;
        } else {
            pharmacyNameLayout.setError(null);
        }
        return isValid;
    }

    private void updatePharmacy() {
        updateButton.setEnabled(false);
        updateButton.setText("Güncelleniyor...");

        String name = pharmacyNameInput.getText().toString().trim();
        String phone = pharmacyPhoneInput.getText().toString().trim();
        String address = pharmacyAddressInput.getText().toString().trim();
        String weekday = weekdayInput.getText().toString().trim();
        String weekend = weekendInput.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        Map<String, String> hours = new HashMap<>();
        hours.put("weekday", weekday);
        hours.put("weekend", weekend);
        updates.put("workingHours", hours);

        if (selectedLocation != null) {
            updates.put("location", new GeoPoint(selectedLocation.latitude, selectedLocation.longitude));
        }

        db.collection("pharmacies").document(pharmacyId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bilgiler güncellendi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    updateButton.setEnabled(true);
                    updateButton.setText("Güncelle");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


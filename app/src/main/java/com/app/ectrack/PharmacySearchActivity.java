package com.app.ectrack;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class PharmacySearchActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FirebaseFirestore db;
    private MaterialToolbar toolbar;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_search);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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

        checkLocationPermission();
        loadPharmacies();

        map.setOnMarkerClickListener(marker -> {
            showPharmacyDetails((DocumentSnapshot) marker.getTag());
            return true;
        });
    }

    private void checkLocationPermission() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (map != null) {
            try {
                map.setMyLocationEnabled(true);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                    }
                });
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }

    private void loadPharmacies() {
        db.collection("pharmacies")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        GeoPoint location = doc.getGeoPoint("location");

                        if (name != null && location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            com.google.android.gms.maps.model.Marker marker = map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name));
                            if (marker != null) {
                                marker.setTag(doc);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showPharmacyDetails(DocumentSnapshot doc) {
        if (doc == null)
            return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        dialog.setContentView(R.layout.dialog_pharmacy_details);

        android.widget.TextView nameText = dialog.findViewById(R.id.textPharmacyName);
        android.widget.TextView addressText = dialog.findViewById(R.id.textPharmacyAddress);
        android.widget.TextView hoursText = dialog.findViewById(R.id.textWorkingHours);
        com.google.android.material.button.MaterialButton btnCall = dialog.findViewById(R.id.btnCall);
        com.google.android.material.button.MaterialButton btnDirections = dialog.findViewById(R.id.btnDirections);

        if (nameText != null)
            nameText.setText(doc.getString("name"));
        if (addressText != null)
            addressText.setText(doc.getString("address"));

        if (hoursText != null && doc.contains("workingHours")) {
            java.util.Map<String, String> hours = (java.util.Map<String, String>) doc.get("workingHours");
            if (hours != null) {
                hoursText.setText("Hafta İçi: " + hours.get("weekday") + "\nHafta Sonu: " + hours.get("weekend"));
            }
        }

        if (btnCall != null) {
            String phone = doc.getString("phone");
            if (phone != null && !phone.isEmpty()) {
                btnCall.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                    intent.setData(android.net.Uri.parse("tel:" + phone));
                    startActivity(intent);
                });
            } else {
                btnCall.setEnabled(false);
                btnCall.setAlpha(0.5f);
            }
        }

        if (btnDirections != null) {
            btnDirections.setOnClickListener(v -> {
                GeoPoint location = doc.getGeoPoint("location");
                if (location != null) {
                    android.net.Uri gmmIntentUri = android.net.Uri
                            .parse("google.navigation:q=" + location.getLatitude() + "," + location.getLongitude());
                    android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                            gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {

                        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri));
                    }
                }
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}


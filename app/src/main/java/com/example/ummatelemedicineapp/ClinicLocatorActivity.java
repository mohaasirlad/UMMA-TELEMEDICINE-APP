package com.example.ummatelemedicineapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClinicLocatorActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final LatLng ummaMain = new LatLng(-1.286389, 36.817223);
    private LatLng userLocation = new LatLng(-1.2800, 36.8150); // Default simulated location
    private TextView tvClinicName, tvClinicAddress;
    private Button btnGetDirections;
    private SearchView searchView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private List<Marker> hospitalMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clinic_locator);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvClinicName = findViewById(R.id.tvClinicName);
        tvClinicAddress = findViewById(R.id.tvClinicAddress);
        btnGetDirections = findViewById(R.id.btnGetDirections);
        searchView = findViewById(R.id.searchView);
        Button btnSearch = findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(v -> {
            String query = searchView.getQuery().toString();
            if (!query.isEmpty()) {
                filterHospitals(query);
                // Hide keyboard after search
                searchView.clearFocus();
            } else {
                Toast.makeText(this, "Please enter a clinic name", Toast.LENGTH_SHORT).show();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnGetDirections.setOnClickListener(v -> {
            if (selectedMarker != null) {
                LatLng destination = selectedMarker.getPosition();
                String uri = String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", 
                        destination.latitude, destination.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    // Fallback to browser if Maps app is not installed
                    String webUri = String.format(Locale.ENGLISH, "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                            destination.latitude, destination.longitude);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
                }
            } else {
                Toast.makeText(this, "Please select a clinic on the map first", Toast.LENGTH_SHORT).show();
            }
        });

        setupSearch();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterHospitals(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterHospitals(newText);
                return true;
            }
        });
    }

    private void filterHospitals(String query) {
        if (mMap == null) return;
        
        if (query.isEmpty()) {
            for (Marker m : hospitalMarkers) m.setVisible(true);
            return;
        }

        Marker firstMatch = null;
        String lowerQuery = query.toLowerCase();

        for (Marker m : hospitalMarkers) {
            boolean isMatch = m.getTitle().toLowerCase().contains(lowerQuery);
            m.setVisible(isMatch);
            if (isMatch && firstMatch == null) {
                firstMatch = m;
            }
        }

        if (firstMatch != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstMatch.getPosition(), 15f));
            updateClinicDetails(firstMatch);
        }
    }

    private Marker selectedMarker;
    private com.google.android.gms.maps.model.Polyline currentRoute;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Professional Map Settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        checkLocationPermission();

        // Add Main UMMA Marker
        Marker mainMarker = mMap.addMarker(new MarkerOptions()
                .position(ummaMain)
                .title("UMMA Health Center - Main")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        hospitalMarkers.add(mainMarker);

        addNearbyHospitals();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f));

        mMap.setOnMarkerClickListener(marker -> {
            updateClinicDetails(marker);
            return true;
        });

        // Check for auto-directions from QueueFragment
        if (getIntent().getBooleanExtra("show_directions_to_main", false)) {
            updateClinicDetails(mainMarker);
            drawMockRoute(userLocation, ummaMain);
            Toast.makeText(this, "Showing route to UMMA Main Clinic", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClinicDetails(Marker marker) {
        if (marker.getTitle().equals("Your Location")) return;

        selectedMarker = marker;
        tvClinicName.setText(marker.getTitle());
        tvClinicAddress.setText(String.format(java.util.Locale.getDefault(), "Location: %.4f, %.4f",
                marker.getPosition().latitude, marker.getPosition().longitude));
        marker.showInfoWindow();

        // Clear previous route when selecting new marker
        if (currentRoute != null) {
            currentRoute.remove();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Using simulated location.", Toast.LENGTH_LONG).show();
                addUserMarker(userLocation);
            }
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                addUserMarker(userLocation);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f));
            } else {
                addUserMarker(userLocation);
            }
        });
    }

    private void addUserMarker(LatLng location) {
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void drawMockRoute(LatLng start, LatLng end) {
        if (mMap == null) return;
        
        if (currentRoute != null) {
            currentRoute.remove();
        }

        com.google.android.gms.maps.model.PolylineOptions options = new com.google.android.gms.maps.model.PolylineOptions()
                .add(start)
                .add(new LatLng((start.latitude + end.latitude) / 2, start.longitude)) // Mock turn
                .add(new LatLng((start.latitude + end.latitude) / 2, end.longitude))   // Mock turn
                .add(end)
                .color(ContextCompat.getColor(this, R.color.primary_blue))
                .width(12);

        currentRoute = mMap.addPolyline(options);
        
        try {
            // Zoom to show both points
            com.google.android.gms.maps.model.LatLngBounds bounds = new com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(start)
                    .include(end)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
        } catch (Exception e) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(end, 14f));
        }
    }

    private void addNearbyHospitals() {
        List<Hospital> hospitals = new ArrayList<>();
        hospitals.add(new Hospital("City View Hospital", new LatLng(-1.2833, 36.8167)));
        hospitals.add(new Hospital("St. Mary's Clinic", new LatLng(-1.2900, 36.8200)));
        hospitals.add(new Hospital("Westlands Medical Center", new LatLng(-1.2650, 36.8080)));
        hospitals.add(new Hospital("Eastleigh Community Hospital", new LatLng(-1.2750, 36.8480)));

        for (Hospital h : hospitals) {
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(h.location)
                    .title(h.name)
                    .snippet("Nearby Medical Facility"));
            hospitalMarkers.add(m);
        }
    }

    private static class Hospital {
        String name;
        LatLng location;

        Hospital(String name, LatLng location) {
            this.name = name;
            this.location = location;
        }
    }
}
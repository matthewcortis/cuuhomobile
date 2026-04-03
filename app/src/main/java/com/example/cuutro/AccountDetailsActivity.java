package com.example.cuutro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.trackasia.android.TrackAsia;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.Style;
import com.trackasia.android.maps.TrackAsiaMap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountDetailsActivity extends AppCompatActivity {

    private static final String MAP_VIEW_STATE_KEY = "account_map_view_state";
    private static final LatLng HANOI = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_ZOOM = 12.8;

    private MapView mapView;
    private TrackAsiaMap trackAsiaMap;
    private TextInputEditText addressEditText;
    private MaterialButton pickPreciseAddressButton;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private CancellationSignal currentLocationCancellation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        TrackAsia.getInstance(getApplicationContext());
        setupLocationPermissionLauncher();

        mapView = findViewById(R.id.account_details_map_view);
        addressEditText = findViewById(R.id.edt_account_details_address);
        pickPreciseAddressButton = findViewById(R.id.btn_pick_precise_address_map);
        if (mapView != null) {
            Bundle mapViewBundle = null;
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
            }
            mapView.onCreate(mapViewBundle);
            mapView.getMapAsync(map -> {
                trackAsiaMap = map;
                map.setStyle(
                        new Style.Builder().fromUri(getString(R.string.trackasia_style_url)),
                        style -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(HANOI, DEFAULT_ZOOM))
                );
            });
        }

        View backButton = findViewById(R.id.btn_account_details_back);
        View cancelButton = findViewById(R.id.btn_account_cancel);
        if (pickPreciseAddressButton != null) {
            pickPreciseAddressButton.setOnClickListener(v -> pickAddressFromCurrentLocation());
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (currentLocationCancellation != null) {
            currentLocationCancellation.cancel();
            currentLocationCancellation = null;
        }
        geocodeExecutor.shutdownNow();
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        trackAsiaMap = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAP_VIEW_STATE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAP_VIEW_STATE_KEY, mapViewBundle);
            }
            mapView.onSaveInstanceState(mapViewBundle);
        }
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean hasFine =
                            Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean hasCoarse =
                            Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (hasFine || hasCoarse) {
                        requestCurrentLocationAndFillAddress();
                    } else {
                        Toast.makeText(
                                this,
                                R.string.account_details_location_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void pickAddressFromCurrentLocation() {
        if (!hasLocationPermission()) {
            if (locationPermissionLauncher != null) {
                locationPermissionLauncher.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
            return;
        }
        requestCurrentLocationAndFillAddress();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocationAndFillAddress() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, R.string.account_details_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            Toast.makeText(this, R.string.account_details_location_disabled, Toast.LENGTH_SHORT).show();
            return;
        }

        Location knownLocation = getMostRecentLastKnownLocation(locationManager);
        if (knownLocation != null) {
            setPickButtonLoading(true);
            resolveAddressFromLocation(knownLocation);
            return;
        }

        String provider = getBestProvider(locationManager);
        if (provider == null) {
            Toast.makeText(this, R.string.account_details_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        setPickButtonLoading(true);
        requestCurrentLocation(locationManager, provider, true);
    }

    private String getBestProvider(LocationManager locationManager) {
        List<String> providers = locationManager.getProviders(true);
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (hasFine && providers.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        return providers.get(0);
    }

    @SuppressLint("MissingPermission")
    private Location getMostRecentLastKnownLocation(LocationManager locationManager) {
        List<String> providers = locationManager.getProviders(true);
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        Location bestLocation = null;
        for (String provider : providers) {
            Location candidate = getLastKnownLocationSafe(locationManager, provider);
            if (candidate == null) {
                continue;
            }
            if (bestLocation == null || candidate.getTime() > bestLocation.getTime()) {
                bestLocation = candidate;
            }
        }
        return bestLocation;
    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocationSafe(LocationManager locationManager, String provider) {
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation(
            LocationManager locationManager,
            String provider,
            boolean allowGpsRetry
    ) {
        if (currentLocationCancellation != null) {
            currentLocationCancellation.cancel();
        }
        currentLocationCancellation = new CancellationSignal();

        LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                currentLocationCancellation,
                ContextCompat.getMainExecutor(this),
                location -> {
                    if (location != null) {
                        resolveAddressFromLocation(location);
                        return;
                    }

                    boolean hasFine = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;
                    if (allowGpsRetry
                            && hasFine
                            && !LocationManager.GPS_PROVIDER.equals(provider)
                            && locationManager.getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
                        requestCurrentLocation(locationManager, LocationManager.GPS_PROVIDER, false);
                        return;
                    }

                    Location fallback = getMostRecentLastKnownLocation(locationManager);
                    if (fallback != null) {
                        resolveAddressFromLocation(fallback);
                        return;
                    }

                    setPickButtonLoading(false);
                    Toast.makeText(
                            this,
                            R.string.account_details_location_unavailable,
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }

    private void resolveAddressFromLocation(Location location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        if (trackAsiaMap != null) {
            trackAsiaMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15.5));
        }

        geocodeExecutor.execute(() -> {
            String resolvedAddress = reverseGeocode(target);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setPickButtonLoading(false);

                if (addressEditText == null) {
                    return;
                }

                String textToShow = resolvedAddress;
                if (textToShow == null || textToShow.isBlank()) {
                    textToShow = getString(
                            R.string.account_details_lat_lng_fallback,
                            target.getLatitude(),
                            target.getLongitude()
                    );
                }
                addressEditText.setText(textToShow);
                if (addressEditText.getText() != null) {
                    addressEditText.setSelection(addressEditText.getText().length());
                }
            });
        });
    }

    @SuppressWarnings("deprecation")
    private String reverseGeocode(LatLng target) {
        if (!Geocoder.isPresent()) {
            return null;
        }

        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    target.getLatitude(),
                    target.getLongitude(),
                    1
            );
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }

            Address address = addresses.get(0);
            if (address.getMaxAddressLineIndex() >= 0) {
                StringBuilder lineBuilder = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) {
                        lineBuilder.append(", ");
                    }
                    lineBuilder.append(address.getAddressLine(i));
                }
                return lineBuilder.toString();
            }
            return address.getFeatureName();
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private void setPickButtonLoading(boolean isLoading) {
        if (pickPreciseAddressButton == null) {
            return;
        }
        pickPreciseAddressButton.setEnabled(!isLoading);
        pickPreciseAddressButton.setText(
                isLoading
                        ? R.string.account_details_pick_location_loading
                        : R.string.account_details_pick_precise_location
        );
    }
}

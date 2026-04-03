package com.example.cuutro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import java.util.regex.Pattern;

public class AccountDetailsActivity extends AppCompatActivity {

    private static final String MAP_VIEW_STATE_KEY = "account_map_view_state";
    private static final String AVATAR_URI_KEY = "account_avatar_uri";
    private static final LatLng HANOI = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_ZOOM = 12.8;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");

    private MapView mapView;
    private TrackAsiaMap trackAsiaMap;
    private AppCompatImageView avatarImageView;
    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout phoneInputLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText addressEditText;
    private MaterialButton saveButton;
    private MaterialButton pickPreciseAddressButton;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> pickAvatarLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private CancellationTokenSource currentLocationTokenSource;
    private Uri selectedAvatarUri;
    private boolean isFormattingPhoneInput;
    private LatLng currentUserLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        TrackAsia.getInstance(getApplicationContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupAvatarPickerLauncher();
        setupLocationPermissionLauncher();

        avatarImageView = findViewById(R.id.img_account_avatar);
        nameInputLayout = findViewById(R.id.account_details_name_input_layout);
        emailInputLayout = findViewById(R.id.account_details_email_input_layout);
        phoneInputLayout = findViewById(R.id.account_details_phone_input_layout);
        nameEditText = findViewById(R.id.edt_account_details_name);
        emailEditText = findViewById(R.id.edt_account_details_email);
        phoneEditText = findViewById(R.id.edt_account_details_phone);
        mapView = findViewById(R.id.account_details_map_view);
        addressEditText = findViewById(R.id.edt_account_details_address);
        saveButton = findViewById(R.id.btn_account_save);
        pickPreciseAddressButton = findViewById(R.id.btn_pick_precise_address_map);
        if (mapView != null) {
            Bundle mapViewBundle = null;
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
            }
            mapView.onCreate(mapViewBundle);
            mapView.getMapAsync(map -> {
                trackAsiaMap = map;
                LatLng initialTarget = currentUserLatLng != null ? currentUserLatLng : HANOI;
                double initialZoom = currentUserLatLng != null ? 15.5 : DEFAULT_ZOOM;
                map.setStyle(
                        new Style.Builder().fromUri(getString(R.string.trackasia_style_url)),
                        style -> map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(initialTarget, initialZoom)
                        )
                );
            });
        }

        restoreAvatarState(savedInstanceState);
        setupFormValidationInteractions();

        View backButton = findViewById(R.id.btn_account_details_back);
        View cancelButton = findViewById(R.id.btn_account_cancel);
        View avatarButton = findViewById(R.id.btn_account_avatar);
        if (avatarButton != null) {
            avatarButton.setOnClickListener(v -> {
                if (pickAvatarLauncher != null) {
                    pickAvatarLauncher.launch("image/*");
                }
            });
        }
        if (pickPreciseAddressButton != null) {
            pickPreciseAddressButton.setOnClickListener(v -> pickAddressFromCurrentLocation());
        }
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                if (validateAccountDetailsInputs()) {
                    Toast.makeText(this, R.string.account_details_saved_success, Toast.LENGTH_SHORT).show();
                }
            });
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
        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
            currentLocationTokenSource = null;
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
        if (selectedAvatarUri != null) {
            outState.putString(AVATAR_URI_KEY, selectedAvatarUri.toString());
        }
        if (mapView != null) {
            Bundle mapViewBundle = outState.getBundle(MAP_VIEW_STATE_KEY);
            if (mapViewBundle == null) {
                mapViewBundle = new Bundle();
                outState.putBundle(MAP_VIEW_STATE_KEY, mapViewBundle);
            }
            mapView.onSaveInstanceState(mapViewBundle);
        }
    }

    private void setupAvatarPickerLauncher() {
        pickAvatarLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri == null) {
                        return;
                    }
                    selectedAvatarUri = uri;
                    applySelectedAvatar(uri);
                });
    }

    private void restoreAvatarState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String avatarUriString = savedInstanceState.getString(AVATAR_URI_KEY);
        if (avatarUriString == null || avatarUriString.isBlank()) {
            return;
        }
        selectedAvatarUri = Uri.parse(avatarUriString);
        applySelectedAvatar(selectedAvatarUri);
    }

    private void applySelectedAvatar(Uri uri) {
        if (avatarImageView == null || uri == null) {
            return;
        }
        avatarImageView.setImageURI(uri);
        avatarImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarImageView.setPadding(0, 0, 0, 0);
        avatarImageView.setImageTintList(null);
    }

    private void setupFormValidationInteractions() {
        if (emailEditText != null) {
            emailEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (emailInputLayout != null) {
                        emailInputLayout.setError(null);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        if (phoneEditText != null) {
            phoneEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (phoneInputLayout != null) {
                        phoneInputLayout.setError(null);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (editable == null || isFormattingPhoneInput || phoneEditText == null) {
                        return;
                    }
                    String raw = editable.toString();
                    String digitsOnly = raw.replaceAll("\\D+", "");
                    if (digitsOnly.length() > 10) {
                        digitsOnly = digitsOnly.substring(0, 10);
                    }
                    if (raw.equals(digitsOnly)) {
                        return;
                    }
                    isFormattingPhoneInput = true;
                    phoneEditText.setText(digitsOnly);
                    phoneEditText.setSelection(digitsOnly.length());
                    isFormattingPhoneInput = false;
                }
            });
        }
    }

    private boolean validateAccountDetailsInputs() {
        boolean isValid = true;

        String name = getTextValue(nameEditText);
        String email = getTextValue(emailEditText);
        String phone = getTextValue(phoneEditText);

        if (nameInputLayout != null) {
            nameInputLayout.setError(null);
            if (name.isEmpty()) {
                nameInputLayout.setError(getString(R.string.account_details_name_required));
                isValid = false;
            }
        }

        if (emailInputLayout != null) {
            emailInputLayout.setError(null);
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.setError(getString(R.string.account_details_email_error));
                isValid = false;
            }
        }

        if (phoneInputLayout != null) {
            phoneInputLayout.setError(null);
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                phoneInputLayout.setError(getString(R.string.account_details_phone_error));
                isValid = false;
            }
        }

        return isValid;
    }

    private String getTextValue(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
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
        if (!isLocationServiceEnabled()) {
            Toast.makeText(this, R.string.account_details_location_disabled, Toast.LENGTH_SHORT).show();
            return;
        }
        if (fusedLocationClient == null) {
            Toast.makeText(this, R.string.account_details_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        setPickButtonLoading(true);
        requestCurrentLocationViaFused();
    }

    private boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocationViaFused() {
        if (fusedLocationClient == null) {
            onLocationRequestFailed();
            return;
        }
        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
        }
        currentLocationTokenSource = new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, currentLocationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        resolveAddressFromLocation(location);
                        return;
                    }
                    requestLastKnownLocationViaFused();
                })
                .addOnFailureListener(e -> requestLastKnownLocationViaFused());
    }

    @SuppressLint("MissingPermission")
    private void requestLastKnownLocationViaFused() {
        if (fusedLocationClient == null) {
            onLocationRequestFailed();
            return;
        }
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        resolveAddressFromLocation(location);
                    } else {
                        onLocationRequestFailed();
                    }
                })
                .addOnFailureListener(e -> onLocationRequestFailed());
    }

    private void onLocationRequestFailed() {
        setPickButtonLoading(false);
        Toast.makeText(
                this,
                R.string.account_details_location_unavailable,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void resolveAddressFromLocation(Location location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        currentUserLatLng = target;
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

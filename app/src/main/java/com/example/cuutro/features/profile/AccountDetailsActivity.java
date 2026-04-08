package com.example.cuutro.features.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
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

import com.example.cuutro.R;
import com.example.cuutro.core.location.DeviceLocationProvider;
import com.example.cuutro.core.location.LocationAddressResolver;
import com.example.cuutro.core.location.MapGestureCoordinator;
import com.example.cuutro.core.location.TrackAsiaMapController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.trackasia.android.annotations.Marker;
import com.trackasia.android.annotations.MarkerOptions;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.TrackAsiaMap;

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
    private TrackAsiaMapController mapController;
    private TrackAsiaMap trackAsiaMap;
    private Marker currentLocationMarker;
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
    private DeviceLocationProvider locationProvider;
    private LocationAddressResolver addressResolver;
    private ActivityResultLauncher<String> pickAvatarLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Uri selectedAvatarUri;
    private boolean isFormattingPhoneInput;
    private LatLng currentUserLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        locationProvider = new DeviceLocationProvider(this);
        addressResolver = new LocationAddressResolver(this, new Locale("vi", "VN"));
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
            MapGestureCoordinator.install(mapView);
            Bundle mapViewBundle = null;
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
            }
            mapController = new TrackAsiaMapController(mapView);
            mapController.onCreate(mapViewBundle);
            LatLng initialTarget = currentUserLatLng != null ? currentUserLatLng : HANOI;
            double initialZoom = currentUserLatLng != null ? 15.5 : DEFAULT_ZOOM;
            mapController.loadStyle(
                    getString(R.string.trackasia_style_url),
                    initialTarget,
                    initialZoom,
                    map -> {
                        trackAsiaMap = map;
                        if (currentUserLatLng != null) {
                            placeOrMoveCurrentLocationMarker(currentUserLatLng, false);
                        }
                    }
            );
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
        if (mapController != null) {
            mapController.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapController != null) {
            mapController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapController != null) {
            mapController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapController != null) {
            mapController.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapController != null) {
            mapController.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (locationProvider != null) {
            locationProvider.cancel();
        }
        geocodeExecutor.shutdownNow();
        if (mapController != null) {
            mapController.onDestroy();
            mapController = null;
        }
        mapView = null;
        trackAsiaMap = null;
        currentLocationMarker = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedAvatarUri != null) {
            outState.putString(AVATAR_URI_KEY, selectedAvatarUri.toString());
        }
        if (mapController != null) {
            mapController.onSaveInstanceState(outState, MAP_VIEW_STATE_KEY);
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
        setPickButtonLoading(true);
        if (locationProvider == null) {
            onLocationRequestFailed(DeviceLocationProvider.Error.LOCATION_UNAVAILABLE);
            return;
        }
        locationProvider.requestCurrentLocation(new DeviceLocationProvider.Callback() {
            @Override
            public void onLocation(@androidx.annotation.NonNull Location location) {
                resolveAddressFromLocation(location);
            }

            @Override
            public void onError(@androidx.annotation.NonNull DeviceLocationProvider.Error error) {
                onLocationRequestFailed(error);
            }
        });
    }

    private void onLocationRequestFailed(DeviceLocationProvider.Error error) {
        setPickButtonLoading(false);
        int messageRes = error == DeviceLocationProvider.Error.LOCATION_DISABLED
                ? R.string.account_details_location_disabled
                : R.string.account_details_location_unavailable;
        Toast.makeText(
                this,
                messageRes,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void resolveAddressFromLocation(Location location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        currentUserLatLng = target;
        placeOrMoveCurrentLocationMarker(target, true);

        geocodeExecutor.execute(() -> {
            String fallback = getString(
                    R.string.account_details_lat_lng_fallback,
                    target.getLatitude(),
                    target.getLongitude()
            );
            String resolvedAddress = addressResolver != null
                    ? addressResolver.reverseGeocode(target, fallback).getFullAddress()
                    : fallback;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setPickButtonLoading(false);

                if (addressEditText == null) {
                    return;
                }

                addressEditText.setText(resolvedAddress);
                if (addressEditText.getText() != null) {
                    addressEditText.setSelection(addressEditText.getText().length());
                }
            });
        });
    }

    private void placeOrMoveCurrentLocationMarker(@androidx.annotation.NonNull LatLng target, boolean animateCamera) {
        if (trackAsiaMap == null) {
            return;
        }

        if (currentLocationMarker != null) {
            trackAsiaMap.removeMarker(currentLocationMarker);
        }
        currentLocationMarker = trackAsiaMap.addMarker(new MarkerOptions().position(target));

        if (animateCamera) {
            trackAsiaMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15.5));
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

package com.example.cuutro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.trackasia.android.annotations.Marker;
import com.trackasia.android.annotations.MarkerOptions;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.Style;
import com.trackasia.android.maps.TrackAsiaMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportActivity extends AppCompatActivity {

    private static final LatLng DEFAULT_MAP_TARGET = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_MAP_ZOOM = 15.2;

    private final List<View> emergencyItemViews = new ArrayList<>();
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    private List<EmergencyType> emergencyTypes;
    private int selectedEmergencyIndex = 0;

    private TextView reportLocationValueText;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private CancellationTokenSource currentLocationTokenSource;

    private BottomSheetDialog locationBottomSheetDialog;
    private MapView bottomSheetMapView;
    private TrackAsiaMap bottomSheetTrackAsiaMap;
    private Marker bottomSheetMarker;
    private MaterialButton bottomSheetPreciseButton;
    private MaterialButton bottomSheetTrackMyLocationButton;
    private TextInputEditText bottomAddressLineEditText;
    private TextInputEditText bottomCityEditText;
    private TextInputEditText bottomPostalEditText;
    private TextView bottomSelectedLocationText;

    private String selectedLocationText;
    private LatLng selectedLocationLatLng = DEFAULT_MAP_TARGET;
    private String pendingBottomSheetLocationText;
    private LatLng pendingBottomSheetLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationPermissionLauncher();

        reportLocationValueText = findViewById(R.id.tvReportLocationValue);
        selectedLocationText = reportLocationValueText != null
                ? reportLocationValueText.getText().toString().trim()
                : getString(R.string.report_location_value);

        setupEmergencyTypeGrid();
        setupActions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
            currentLocationTokenSource = null;
        }
        geocodeExecutor.shutdownNow();
        releaseLocationBottomSheetMap();
        super.onDestroy();
    }

    private void setupActions() {
        ImageButton backButton = findViewById(R.id.btnBack);
        MaterialButton submitButton = findViewById(R.id.btnSubmitReport);
        TextView changeLocationText = findViewById(R.id.tvChangeLocation);

        backButton.setOnClickListener(v -> finish());
        changeLocationText.setOnClickListener(v -> openLocationBottomSheet());
        submitButton.setOnClickListener(v -> {
            String selectedLabel = emergencyTypes.get(selectedEmergencyIndex).label;
            Toast.makeText(
                    this,
                    getString(R.string.report_selected_type, selectedLabel),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupEmergencyTypeGrid() {
        GridLayout gridLayout = findViewById(R.id.gridEmergencyTypes);
        emergencyTypes = buildEmergencyTypes();
        LayoutInflater inflater = LayoutInflater.from(this);

        gridLayout.removeAllViews();
        emergencyItemViews.clear();

        for (int i = 0; i < emergencyTypes.size(); i++) {
            EmergencyType emergencyType = emergencyTypes.get(i);
            View itemView = inflater.inflate(R.layout.item_emergency_type, gridLayout, false);

            EmergencyTypeViewHolder holder = new EmergencyTypeViewHolder(itemView);
            holder.iconView.setImageResource(emergencyType.iconResId);
            holder.nameView.setText(emergencyType.label);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
            );
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            int horizontalMargin = dpToPx(2);
            int verticalMargin = dpToPx(8);
            params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            itemView.setLayoutParams(params);

            int position = i;
            itemView.setOnClickListener(v -> selectEmergencyType(position));

            itemView.setTag(holder);
            emergencyItemViews.add(itemView);
            gridLayout.addView(itemView);
        }

        selectEmergencyType(selectedEmergencyIndex);
    }

    private List<EmergencyType> buildEmergencyTypes() {
        return Arrays.asList(
                new EmergencyType(getString(R.string.report_accident), R.drawable.ic_emergency_accident),
                new EmergencyType(getString(R.string.report_fire), R.drawable.ic_emergency_fire),
                new EmergencyType(getString(R.string.report_medical), R.drawable.ic_emergency_medical),
                new EmergencyType(getString(R.string.report_flood), R.drawable.ic_emergency_flood),
                new EmergencyType(getString(R.string.report_quake), R.drawable.ic_emergency_quake),
                new EmergencyType(getString(R.string.report_robbery), R.drawable.ic_emergency_robbery),
                new EmergencyType(getString(R.string.report_assault), R.drawable.ic_emergency_assault),
                new EmergencyType(getString(R.string.report_other), R.drawable.ic_emergency_other)
        );
    }

    private void selectEmergencyType(int position) {
        selectedEmergencyIndex = position;

        int selectedTextColor = ContextCompat.getColor(this, R.color.report_text_primary);
        int unselectedTextColor = ContextCompat.getColor(this, R.color.report_label_inactive);
        int selectedIconTint = ContextCompat.getColor(this, R.color.white);
        int unselectedIconTint = ContextCompat.getColor(this, R.color.report_icon_inactive);

        for (int i = 0; i < emergencyItemViews.size(); i++) {
            View itemView = emergencyItemViews.get(i);
            EmergencyTypeViewHolder holder = (EmergencyTypeViewHolder) itemView.getTag();
            boolean isSelected = i == position;

            holder.iconContainer.setBackgroundResource(
                    isSelected
                            ? R.drawable.bg_report_emergency_icon_selected
                            : R.drawable.bg_report_emergency_icon_unselected
            );
            holder.iconView.setImageTintList(
                    ColorStateList.valueOf(isSelected ? selectedIconTint : unselectedIconTint)
            );
            holder.nameView.setTextColor(isSelected ? selectedTextColor : unselectedTextColor);
        }
    }

    private void openLocationBottomSheet() {
        if (locationBottomSheetDialog != null && locationBottomSheetDialog.isShowing()) {
            return;
        }

        View bottomSheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_select_location, null, false);

        bindBottomSheetViews(bottomSheetView);
        pendingBottomSheetLocationText = selectedLocationText;
        pendingBottomSheetLatLng = selectedLocationLatLng;
        populateBottomSheetInputsFromSelectedLocation();
        setupBottomSheetActions(bottomSheetView);
        setupBottomSheetMap(bottomSheetView);

        locationBottomSheetDialog = new BottomSheetDialog(this);
        locationBottomSheetDialog.setContentView(bottomSheetView);
        locationBottomSheetDialog.setOnDismissListener(dialog -> releaseLocationBottomSheetMap());
        locationBottomSheetDialog.show();

        FrameLayout bottomSheet =
                locationBottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    private void bindBottomSheetViews(View root) {
        bottomSheetPreciseButton = root.findViewById(R.id.btnUsePreciseLocation);
        bottomSheetTrackMyLocationButton = root.findViewById(R.id.btnTrackMyLocation);
        bottomAddressLineEditText = root.findViewById(R.id.edtBottomAddressLine);
        bottomCityEditText = root.findViewById(R.id.edtBottomCity);
        bottomPostalEditText = root.findViewById(R.id.edtBottomPostal);
        bottomSelectedLocationText = root.findViewById(R.id.tvBottomSelectedLocation);
    }

    private void setupBottomSheetActions(View root) {
        ImageButton closeButton = root.findViewById(R.id.btnCloseLocationSheet);
        MaterialButton pickTrackMapButton = root.findViewById(R.id.btnPickFromTrackMap);
        MaterialButton confirmButton = root.findViewById(R.id.btnConfirmLocation);

        closeButton.setOnClickListener(v -> {
            if (locationBottomSheetDialog != null) {
                locationBottomSheetDialog.dismiss();
            }
        });

        if (bottomSheetPreciseButton != null) {
            bottomSheetPreciseButton.setOnClickListener(v -> requestCurrentLocationForBottomSheet());
        }
        if (bottomSheetTrackMyLocationButton != null) {
            bottomSheetTrackMyLocationButton.setOnClickListener(v -> requestCurrentLocationForBottomSheet());
        }
        pickTrackMapButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.report_tap_on_map_hint, Toast.LENGTH_SHORT).show()
        );
        confirmButton.setOnClickListener(v -> confirmSelectedLocation());
    }

    private void setupBottomSheetMap(View root) {
        bottomSheetMapView = root.findViewById(R.id.report_location_map_view);
        if (bottomSheetMapView == null) {
            return;
        }

        bottomSheetMapView.onCreate(null);
        bottomSheetMapView.onStart();
        bottomSheetMapView.onResume();

        bottomSheetMapView.getMapAsync(map -> {
            bottomSheetTrackAsiaMap = map;
            map.setStyle(
                    new Style.Builder().fromUri(getString(R.string.trackasia_style_url)),
                    style -> {
                        map.addOnMapClickListener(point -> {
                            onMapPointPicked(point, true);
                            return true;
                        });

                        LatLng initialTarget = selectedLocationLatLng != null
                                ? selectedLocationLatLng
                                : DEFAULT_MAP_TARGET;
                        if (pendingBottomSheetLatLng != null) {
                            initialTarget = pendingBottomSheetLatLng;
                        }
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialTarget, DEFAULT_MAP_ZOOM));
                        placeOrMoveBottomSheetMarker(initialTarget, false);
                    }
            );
        });
    }

    private void confirmSelectedLocation() {
        String newAddressLine = getTextValue(bottomAddressLineEditText);
        String newCity = getTextValue(bottomCityEditText);
        String newPostal = getTextValue(bottomPostalEditText);

        String composed = composeLocationText(newAddressLine, newCity, newPostal);
        if (composed.isBlank()) {
            composed = pendingBottomSheetLocationText;
        }

        selectedLocationText = composed;
        if (pendingBottomSheetLatLng != null) {
            selectedLocationLatLng = pendingBottomSheetLatLng;
        }
        if (reportLocationValueText != null) {
            reportLocationValueText.setText(composed);
        }

        if (locationBottomSheetDialog != null) {
            locationBottomSheetDialog.dismiss();
        }
    }

    private void populateBottomSheetInputsFromSelectedLocation() {
        if (bottomSelectedLocationText != null) {
            bottomSelectedLocationText.setText(pendingBottomSheetLocationText);
        }

        String[] parts = pendingBottomSheetLocationText == null
                ? new String[0]
                : pendingBottomSheetLocationText.split(",");
        if (bottomAddressLineEditText != null && parts.length > 0) {
            bottomAddressLineEditText.setText(parts[0].trim());
        }
        if (bottomCityEditText != null && parts.length > 1) {
            bottomCityEditText.setText(parts[1].trim());
        }
        if (bottomPostalEditText != null && parts.length > 2) {
            bottomPostalEditText.setText(parts[2].trim());
        }
    }

    private void onMapPointPicked(LatLng target, boolean animateCamera) {
        pendingBottomSheetLatLng = target;
        placeOrMoveBottomSheetMarker(target, animateCamera);
        resolveLocationAddress(target);
    }

    private void placeOrMoveBottomSheetMarker(LatLng target, boolean animateCamera) {
        if (bottomSheetTrackAsiaMap == null || target == null) {
            return;
        }

        if (bottomSheetMarker != null) {
            bottomSheetTrackAsiaMap.removeMarker(bottomSheetMarker);
        }

        bottomSheetMarker = bottomSheetTrackAsiaMap.addMarker(new MarkerOptions().position(target));

        if (animateCamera) {
            bottomSheetTrackAsiaMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, DEFAULT_MAP_ZOOM)
            );
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
                        requestCurrentLocationAndResolve();
                    } else {
                        Toast.makeText(
                                this,
                                R.string.report_location_permission_denied,
                                Toast.LENGTH_SHORT
                        ).show();
                        setBottomSheetLocationLoading(false);
                    }
                });
    }

    private void requestCurrentLocationForBottomSheet() {
        if (!hasLocationPermission()) {
            setBottomSheetLocationLoading(true);
            if (locationPermissionLauncher != null) {
                locationPermissionLauncher.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
            return;
        }

        requestCurrentLocationAndResolve();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocationAndResolve() {
        if (!isLocationServiceEnabled()) {
            Toast.makeText(this, R.string.report_location_disabled, Toast.LENGTH_SHORT).show();
            setBottomSheetLocationLoading(false);
            return;
        }
        if (fusedLocationClient == null) {
            Toast.makeText(this, R.string.report_location_unavailable, Toast.LENGTH_SHORT).show();
            setBottomSheetLocationLoading(false);
            return;
        }

        setBottomSheetLocationLoading(true);

        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
        }
        currentLocationTokenSource = new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, currentLocationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onDeviceLocationResolved(location);
                    } else {
                        requestLastKnownLocation();
                    }
                })
                .addOnFailureListener(e -> requestLastKnownLocation());
    }

    @SuppressLint("MissingPermission")
    private void requestLastKnownLocation() {
        if (fusedLocationClient == null) {
            onLocationRequestFailed();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        onDeviceLocationResolved(location);
                    } else {
                        onLocationRequestFailed();
                    }
                })
                .addOnFailureListener(e -> onLocationRequestFailed());
    }

    private void onDeviceLocationResolved(Location location) {
        LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
        onMapPointPicked(target, true);
    }

    private void onLocationRequestFailed() {
        setBottomSheetLocationLoading(false);
        Toast.makeText(this, R.string.report_location_unavailable, Toast.LENGTH_SHORT).show();
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

    private void resolveLocationAddress(LatLng target) {
        geocodeExecutor.execute(() -> {
            LocationSelection selection = reverseGeocode(target);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                applyResolvedLocation(selection, target);
            });
        });
    }

    private void applyResolvedLocation(LocationSelection selection, LatLng target) {
        setBottomSheetLocationLoading(false);

        String fullAddress = selection.fullAddress;
        if (fullAddress == null || fullAddress.isBlank()) {
            fullAddress = getString(
                    R.string.report_lat_lng_fallback,
                    target.getLatitude(),
                    target.getLongitude()
            );
        }

        pendingBottomSheetLocationText = fullAddress;

        if (bottomSelectedLocationText != null) {
            bottomSelectedLocationText.setText(fullAddress);
        }
        if (bottomAddressLineEditText != null) {
            bottomAddressLineEditText.setText(selection.addressLine);
        }
        if (bottomCityEditText != null) {
            bottomCityEditText.setText(selection.city);
        }
        if (bottomPostalEditText != null) {
            bottomPostalEditText.setText(selection.postalCode);
        }
    }

    @SuppressWarnings("deprecation")
    private LocationSelection reverseGeocode(LatLng target) {
        String fallback = getString(
                R.string.report_lat_lng_fallback,
                target.getLatitude(),
                target.getLongitude()
        );

        if (!Geocoder.isPresent()) {
            return new LocationSelection(fallback, fallback, "", "");
        }

        Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    target.getLatitude(),
                    target.getLongitude(),
                    1
            );

            if (addresses == null || addresses.isEmpty()) {
                return new LocationSelection(fallback, fallback, "", "");
            }

            Address address = addresses.get(0);
            String fullAddress = buildFullAddress(address);
            String addressLine = firstNonBlank(
                    address.getSubLocality(),
                    address.getThoroughfare(),
                    address.getFeatureName(),
                    splitAddressSegment(fullAddress, 0)
            );
            String city = firstNonBlank(
                    address.getSubAdminArea(),
                    address.getLocality(),
                    address.getAdminArea(),
                    splitAddressSegment(fullAddress, 1)
            );
            String postalCode = firstNonBlank(
                    address.getPostalCode(),
                    splitAddressSegment(fullAddress, 2)
            );

            return new LocationSelection(
                    fullAddress == null || fullAddress.isBlank() ? fallback : fullAddress,
                    addressLine,
                    city,
                    postalCode
            );
        } catch (IOException | IllegalArgumentException ignored) {
            return new LocationSelection(fallback, fallback, "", "");
        }
    }

    private String buildFullAddress(Address address) {
        if (address == null) {
            return "";
        }
        if (address.getMaxAddressLineIndex() >= 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                String line = address.getAddressLine(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(line.trim());
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }
        return firstNonBlank(address.getFeatureName(), "");
    }

    private String splitAddressSegment(String fullAddress, int index) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return "";
        }
        String[] parts = fullAddress.split(",");
        if (index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index].trim();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    private void setBottomSheetLocationLoading(boolean isLoading) {
        if (bottomSheetPreciseButton != null) {
            bottomSheetPreciseButton.setEnabled(!isLoading);
            bottomSheetPreciseButton.setText(
                    isLoading
                            ? R.string.report_location_loading
                            : R.string.report_use_precise_location
            );
        }
        if (bottomSheetTrackMyLocationButton != null) {
            bottomSheetTrackMyLocationButton.setEnabled(!isLoading);
            bottomSheetTrackMyLocationButton.setText(
                    isLoading
                            ? R.string.report_location_loading
                            : R.string.report_track_my_location
            );
        }
    }

    private String composeLocationText(String addressLine, String city, String postal) {
        StringBuilder builder = new StringBuilder();
        appendLocationPart(builder, addressLine);
        appendLocationPart(builder, city);
        appendLocationPart(builder, postal);
        return builder.toString();
    }

    private void appendLocationPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private String getTextValue(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void releaseLocationBottomSheetMap() {
        if (bottomSheetMapView != null) {
            bottomSheetMapView.onPause();
            bottomSheetMapView.onStop();
            bottomSheetMapView.onDestroy();
            bottomSheetMapView = null;
        }

        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
            currentLocationTokenSource = null;
        }

        bottomSheetTrackAsiaMap = null;
        bottomSheetMarker = null;
        bottomSheetPreciseButton = null;
        bottomSheetTrackMyLocationButton = null;
        bottomAddressLineEditText = null;
        bottomCityEditText = null;
        bottomPostalEditText = null;
        bottomSelectedLocationText = null;
        pendingBottomSheetLocationText = null;
        pendingBottomSheetLatLng = null;
        locationBottomSheetDialog = null;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class EmergencyType {
        final String label;
        final int iconResId;

        EmergencyType(String label, int iconResId) {
            this.label = label;
            this.iconResId = iconResId;
        }
    }

    private static class EmergencyTypeViewHolder {
        final FrameLayout iconContainer;
        final ImageView iconView;
        final TextView nameView;

        EmergencyTypeViewHolder(View itemView) {
            iconContainer = itemView.findViewById(R.id.flEmergencyIconContainer);
            iconView = itemView.findViewById(R.id.ivEmergencyIcon);
            nameView = itemView.findViewById(R.id.tvEmergencyName);
        }
    }

    private static class LocationSelection {
        final String fullAddress;
        final String addressLine;
        final String city;
        final String postalCode;

        LocationSelection(String fullAddress, String addressLine, String city, String postalCode) {
            this.fullAddress = fullAddress;
            this.addressLine = addressLine;
            this.city = city;
            this.postalCode = postalCode;
        }
    }
}

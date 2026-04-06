package com.example.cuutro.location;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cuutro.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.trackasia.android.annotations.Marker;
import com.trackasia.android.annotations.MarkerOptions;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.TrackAsiaMap;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportLocationBottomSheet {

    public interface Listener {
        boolean hasLocationPermission();

        void requestLocationPermission();

        void onLocationConfirmed(@NonNull String locationText, @Nullable LatLng latLng);
    }

    private static final LatLng DEFAULT_MAP_TARGET = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_MAP_ZOOM = 15.2;

    private final AppCompatActivity activity;
    private final Listener listener;
    private final String styleUrl;
    private final DeviceLocationProvider locationProvider;
    private final LocationAddressResolver addressResolver;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    private BottomSheetDialog dialog;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private TrackAsiaMapController mapController;
    private TrackAsiaMap trackAsiaMap;
    private Marker marker;

    private MaterialButton trackMyLocationButton;
    private TextInputEditText addressLineEditText;
    private TextInputEditText cityEditText;
    private TextInputEditText postalEditText;
    private TextView selectedLocationTextView;

    private String selectedLocationText;
    private LatLng selectedLocationLatLng;
    private String pendingLocationText;
    private LatLng pendingLatLng;

    public ReportLocationBottomSheet(
            @NonNull AppCompatActivity activity,
            @NonNull String styleUrl,
            @NonNull Listener listener
    ) {
        this.activity = activity;
        this.styleUrl = styleUrl;
        this.listener = listener;
        this.locationProvider = new DeviceLocationProvider(activity);
        this.addressResolver = new LocationAddressResolver(activity, new Locale("vi", "VN"));
    }

    public void show(@NonNull String currentLocationText, @Nullable LatLng currentLocationLatLng) {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        selectedLocationText = currentLocationText;
        selectedLocationLatLng = currentLocationLatLng != null ? currentLocationLatLng : DEFAULT_MAP_TARGET;
        pendingLocationText = selectedLocationText;
        pendingLatLng = selectedLocationLatLng;

        View contentView = LayoutInflater.from(activity).inflate(
                R.layout.bottom_sheet_select_location,
                null,
                false
        );

        bindViews(contentView);
        populateInputsFromSelectedLocation();
        setupActions(contentView);
        setupMap(contentView);

        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(contentView);
        dialog.setOnDismissListener(d -> releaseMapResources());
        dialog.show();

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetBehavior.setSkipCollapsed(true);
        }
    }

    public void onLocationPermissionResult(boolean granted) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }

        if (granted) {
            requestCurrentLocationInternal();
        } else {
            setLocationLoading(false);
            Toast.makeText(activity, R.string.report_location_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    public void onStart() {
        if (mapController != null) {
            mapController.onStart();
        }
    }

    public void onResume() {
        if (mapController != null) {
            mapController.onResume();
        }
    }

    public void onPause() {
        if (mapController != null) {
            mapController.onPause();
        }
    }

    public void onStop() {
        if (mapController != null) {
            mapController.onStop();
        }
    }

    public void onLowMemory() {
        if (mapController != null) {
            mapController.onLowMemory();
        }
    }

    public void onDestroy() {
        releaseMapResources();
        geocodeExecutor.shutdownNow();
        locationProvider.cancel();
    }

    private void bindViews(@NonNull View root) {
        trackMyLocationButton = root.findViewById(R.id.btnTrackMyLocation);
        addressLineEditText = root.findViewById(R.id.edtBottomAddressLine);
        cityEditText = root.findViewById(R.id.edtBottomCity);
        postalEditText = root.findViewById(R.id.edtBottomPostal);
        selectedLocationTextView = root.findViewById(R.id.tvBottomSelectedLocation);
    }

    private void setupActions(@NonNull View root) {
        ImageButton closeButton = root.findViewById(R.id.btnCloseLocationSheet);
        MaterialButton confirmButton = root.findViewById(R.id.btnConfirmLocation);

        closeButton.setOnClickListener(v -> dismiss());
        confirmButton.setOnClickListener(v -> confirmLocation());

        if (trackMyLocationButton != null) {
            trackMyLocationButton.setOnClickListener(v -> requestCurrentLocation());
        }
    }

    private void setupMap(@NonNull View root) {
        MapView mapView = root.findViewById(R.id.report_location_map_view);
        if (mapView == null) {
            return;
        }
        MapGestureCoordinator.install(mapView, isInteracting -> {
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setDraggable(!isInteracting);
            }
        });

        mapController = new TrackAsiaMapController(mapView);
        mapController.onCreate(null);
        mapController.onStart();
        mapController.onResume();

        LatLng initialTarget = pendingLatLng != null ? pendingLatLng : DEFAULT_MAP_TARGET;
        final LatLng initialTargetFinal = initialTarget;

        mapController.loadStyle(styleUrl, initialTarget, DEFAULT_MAP_ZOOM, map -> {
            trackAsiaMap = map;
            map.addOnMapClickListener(point -> {
                onMapPointPicked(point, true);
                return true;
            });
            placeOrMoveMarker(initialTargetFinal, false);
        });
    }

    private void requestCurrentLocation() {
        if (!listener.hasLocationPermission()) {
            setLocationLoading(true);
            listener.requestLocationPermission();
            return;
        }
        requestCurrentLocationInternal();
    }

    private void requestCurrentLocationInternal() {
        setLocationLoading(true);
        locationProvider.requestCurrentLocation(new DeviceLocationProvider.Callback() {
            @Override
            public void onLocation(@NonNull Location location) {
                LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
                onMapPointPicked(target, true);
            }

            @Override
            public void onError(@NonNull DeviceLocationProvider.Error error) {
                setLocationLoading(false);
                int messageRes = error == DeviceLocationProvider.Error.LOCATION_DISABLED
                        ? R.string.report_location_disabled
                        : R.string.report_location_unavailable;
                Toast.makeText(activity, messageRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onMapPointPicked(@NonNull LatLng target, boolean animateCamera) {
        pendingLatLng = target;
        placeOrMoveMarker(target, animateCamera);
        resolveAddress(target);
    }

    private void placeOrMoveMarker(@NonNull LatLng target, boolean animateCamera) {
        if (trackAsiaMap == null) {
            return;
        }

        if (marker != null) {
            trackAsiaMap.removeMarker(marker);
        }

        marker = trackAsiaMap.addMarker(new MarkerOptions().position(target));

        if (animateCamera) {
            trackAsiaMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, DEFAULT_MAP_ZOOM));
        }
    }

    private void resolveAddress(@NonNull LatLng target) {
        geocodeExecutor.execute(() -> {
            String fallback = activity.getString(
                    R.string.report_lat_lng_fallback,
                    target.getLatitude(),
                    target.getLongitude()
            );
            LocationAddressData data = addressResolver.reverseGeocode(target, fallback);

            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                setLocationLoading(false);

                String fullAddress = data.getFullAddress();
                if (fullAddress.isBlank()) {
                    fullAddress = fallback;
                }
                pendingLocationText = fullAddress;

                if (selectedLocationTextView != null) {
                    selectedLocationTextView.setText(fullAddress);
                }
                if (addressLineEditText != null) {
                    addressLineEditText.setText(data.getAddressLine());
                }
                if (cityEditText != null) {
                    cityEditText.setText(data.getCity());
                }
                if (postalEditText != null) {
                    postalEditText.setText(data.getPostalCode());
                }
            });
        });
    }

    private void populateInputsFromSelectedLocation() {
        if (selectedLocationTextView != null) {
            selectedLocationTextView.setText(pendingLocationText);
        }

        String[] parts = pendingLocationText == null ? new String[0] : pendingLocationText.split(",");
        if (addressLineEditText != null && parts.length > 0) {
            addressLineEditText.setText(parts[0].trim());
        }
        if (cityEditText != null && parts.length > 1) {
            cityEditText.setText(parts[1].trim());
        }
        if (postalEditText != null && parts.length > 2) {
            postalEditText.setText(parts[2].trim());
        }
    }

    private void confirmLocation() {
        String addressLine = getText(addressLineEditText);
        String city = getText(cityEditText);
        String postal = getText(postalEditText);

        String composed = composeLocationText(addressLine, city, postal);
        if (composed.isBlank()) {
            composed = pendingLocationText != null ? pendingLocationText : selectedLocationText;
        }

        LatLng finalLatLng = pendingLatLng != null ? pendingLatLng : selectedLocationLatLng;
        listener.onLocationConfirmed(composed, finalLatLng);
        dismiss();
    }

    private String composeLocationText(String addressLine, String city, String postal) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, addressLine);
        appendPart(builder, city);
        appendPart(builder, postal);
        return builder.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private String getText(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void setLocationLoading(boolean isLoading) {
        if (trackMyLocationButton == null) {
            return;
        }
        trackMyLocationButton.setEnabled(!isLoading);
        trackMyLocationButton.setText(
                isLoading
                        ? R.string.report_location_loading
                        : R.string.report_track_my_location
        );
    }

    private void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void releaseMapResources() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setDraggable(true);
        }

        if (mapController != null) {
            mapController.onPause();
            mapController.onStop();
            mapController.onDestroy();
            mapController = null;
        }

        locationProvider.cancel();

        trackAsiaMap = null;
        marker = null;
        bottomSheetBehavior = null;
        trackMyLocationButton = null;
        addressLineEditText = null;
        cityEditText = null;
        postalEditText = null;
        selectedLocationTextView = null;
        pendingLocationText = null;
        pendingLatLng = null;
        dialog = null;
    }
}

package com.example.cuutro.core.location;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceLocationProvider {

    private static final String TAG = "DeviceLocationProvider";

    public enum Error {
        LOCATION_DISABLED,
        LOCATION_UNAVAILABLE
    }

    public interface Callback {
        void onLocation(@NonNull Location location);

        void onError(@NonNull Error error);
    }

    private static final long SINGLE_UPDATE_INTERVAL_MS = 1_000L;
    private static final long SINGLE_UPDATE_TIMEOUT_MS = 12_000L;
    private static final long PLATFORM_UPDATE_TIMEOUT_MS = 12_000L;

    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CancellationTokenSource currentLocationTokenSource;
    private LocationCallback singleUpdateCallback;
    private Runnable singleUpdateTimeoutRunnable;
    private LocationListener platformLocationListener;
    private Runnable platformLocationTimeoutRunnable;

    public DeviceLocationProvider(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
    }

    public boolean isLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
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
    public void requestCurrentLocation(@NonNull Callback callback) {
        if (!isLocationServiceEnabled()) {
            Log.w(TAG, "Location service disabled");
            callback.onError(Error.LOCATION_DISABLED);
            return;
        }

        cancel();

        AtomicBoolean isHandled = new AtomicBoolean(false);
        Callback safeCallback = new Callback() {
            @Override
            public void onLocation(@NonNull Location location) {
                if (!isHandled.compareAndSet(false, true)) {
                    return;
                }
                cancel();
                callback.onLocation(location);
            }

            @Override
            public void onError(@NonNull Error error) {
                if (!isHandled.compareAndSet(false, true)) {
                    return;
                }
                cancel();
                callback.onError(error);
            }
        };

        currentLocationTokenSource = new CancellationTokenSource();
        int priority = resolvePriority();
        Log.d(TAG, "Request current location with priority=" + priority);

        try {
            fusedLocationClient
                    .getCurrentLocation(priority, currentLocationTokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "getCurrentLocation success");
                            safeCallback.onLocation(location);
                            return;
                        }
                        Log.d(TAG, "getCurrentLocation returned null, fallback single update");
                        requestSingleLocationUpdate(priority, safeCallback);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getCurrentLocation failed, fallback single update", e);
                        requestSingleLocationUpdate(priority, safeCallback);
                    });
        } catch (SecurityException ignored) {
            Log.w(TAG, "SecurityException when requesting current location");
            safeCallback.onError(Error.LOCATION_UNAVAILABLE);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestSingleLocationUpdate(int priority, @NonNull Callback callback) {
        clearSingleUpdateRequest();

        LocationRequest request = new LocationRequest.Builder(priority, SINGLE_UPDATE_INTERVAL_MS)
                .setMaxUpdates(1)
                .setMinUpdateIntervalMillis(0)
                .build();

        singleUpdateCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location == null && !result.getLocations().isEmpty()) {
                    location = result.getLocations().get(result.getLocations().size() - 1);
                }
                clearSingleUpdateRequest();
                if (location != null) {
                    Log.d(TAG, "single update success");
                    callback.onLocation(location);
                    return;
                }
                Log.d(TAG, "single update returned null, fallback last known");
                requestLastKnownLocation(callback);
            }
        };

        singleUpdateTimeoutRunnable = () -> {
            Log.d(TAG, "single update timeout, fallback last known");
            clearSingleUpdateRequest();
            requestLastKnownLocation(callback);
        };
        mainHandler.postDelayed(singleUpdateTimeoutRunnable, SINGLE_UPDATE_TIMEOUT_MS);

        try {
            fusedLocationClient
                    .requestLocationUpdates(request, singleUpdateCallback, Looper.getMainLooper())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "single update failed, fallback last known", e);
                        clearSingleUpdateRequest();
                        requestLastKnownLocation(callback);
                    });
        } catch (SecurityException ignored) {
            Log.w(TAG, "SecurityException when requesting single update");
            clearSingleUpdateRequest();
            callback.onError(Error.LOCATION_UNAVAILABLE);
        }
    }

    private int resolvePriority() {
        return hasFineLocationPermission()
                ? Priority.PRIORITY_HIGH_ACCURACY
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
    }

    @SuppressLint("MissingPermission")
    private void requestLastKnownLocation(@NonNull Callback callback) {
        try {
            fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "fused last known success");
                            callback.onLocation(location);
                        } else {
                            Log.d(TAG, "fused last known null, fallback platform location");
                            requestPlatformLocationUpdate(callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "fused last known failed, fallback platform location", e);
                        requestPlatformLocationUpdate(callback);
                    });
        } catch (SecurityException ignored) {
            Log.w(TAG, "SecurityException when requesting fused last known");
            requestPlatformLocationUpdate(callback);
        }
    }

    private void clearSingleUpdateRequest() {
        if (singleUpdateTimeoutRunnable != null) {
            mainHandler.removeCallbacks(singleUpdateTimeoutRunnable);
            singleUpdateTimeoutRunnable = null;
        }
        if (singleUpdateCallback != null) {
            fusedLocationClient.removeLocationUpdates(singleUpdateCallback);
            singleUpdateCallback = null;
        }
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void requestPlatformLocationUpdate(@NonNull Callback callback) {
        clearPlatformLocationRequest();

        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onError(Error.LOCATION_UNAVAILABLE);
            return;
        }

        String provider = resolveBestProvider(locationManager);
        if (provider == null) {
            Log.w(TAG, "No enabled provider for platform updates, trying platform last known");
            Location fallback = getBestPlatformLastKnownLocation(locationManager);
            if (fallback != null) {
                Log.d(TAG, "platform last known success");
                callback.onLocation(fallback);
            } else {
                Log.w(TAG, "platform last known unavailable");
                callback.onError(Error.LOCATION_UNAVAILABLE);
            }
            return;
        }
        Log.d(TAG, "request platform updates with provider=" + provider);

        platformLocationListener = location -> {
            if (location == null) {
                return;
            }
            Log.d(TAG, "platform update success from provider");
            clearPlatformLocationRequest();
            callback.onLocation(location);
        };

        platformLocationTimeoutRunnable = () -> {
            Log.d(TAG, "platform update timeout, try platform last known");
            clearPlatformLocationRequest();
            Location fallback = getBestPlatformLastKnownLocation(locationManager);
            if (fallback != null) {
                Log.d(TAG, "platform last known success after timeout");
                callback.onLocation(fallback);
            } else {
                Log.w(TAG, "platform unavailable after timeout");
                callback.onError(Error.LOCATION_UNAVAILABLE);
            }
        };
        mainHandler.postDelayed(platformLocationTimeoutRunnable, PLATFORM_UPDATE_TIMEOUT_MS);

        try {
            locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    platformLocationListener,
                    Looper.getMainLooper()
            );
        } catch (SecurityException ignored) {
            Log.w(TAG, "SecurityException when requesting platform updates");
            clearPlatformLocationRequest();
            callback.onError(Error.LOCATION_UNAVAILABLE);
        }
    }

    @Nullable
    private String resolveBestProvider(@NonNull LocationManager locationManager) {
        if (hasFineLocationPermission() && isProviderEnabled(locationManager, LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if ((hasFineLocationPermission() || hasCoarseLocationPermission())
                && isProviderEnabled(locationManager, LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if ((hasFineLocationPermission() || hasCoarseLocationPermission())
                && isProviderEnabled(locationManager, LocationManager.PASSIVE_PROVIDER)) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        return null;
    }

    @Nullable
    private Location getBestPlatformLastKnownLocation(@NonNull LocationManager locationManager) {
        Location bestLocation = null;
        for (String provider : new String[] {
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        }) {
            if (!isProviderEnabled(locationManager, provider)) {
                continue;
            }
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }
                if (bestLocation == null || location.getTime() > bestLocation.getTime()) {
                    bestLocation = location;
                }
            } catch (SecurityException ignored) {
                // Ignore and keep trying remaining providers.
            }
        }
        return bestLocation;
    }

    private boolean isProviderEnabled(@NonNull LocationManager locationManager, @NonNull String provider) {
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void clearPlatformLocationRequest() {
        if (platformLocationTimeoutRunnable != null) {
            mainHandler.removeCallbacks(platformLocationTimeoutRunnable);
            platformLocationTimeoutRunnable = null;
        }

        if (platformLocationListener != null) {
            LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                try {
                    locationManager.removeUpdates(platformLocationListener);
                } catch (Exception ignored) {
                    // Best effort cleanup.
                }
            }
            platformLocationListener = null;
        }
    }

    public void cancel() {
        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
            currentLocationTokenSource = null;
        }
        clearSingleUpdateRequest();
        clearPlatformLocationRequest();
    }
}

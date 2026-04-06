package com.example.cuutro.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class DeviceLocationProvider {

    public enum Error {
        LOCATION_DISABLED,
        LOCATION_UNAVAILABLE
    }

    public interface Callback {
        void onLocation(@NonNull Location location);

        void onError(@NonNull Error error);
    }

    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource currentLocationTokenSource;

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
            callback.onError(Error.LOCATION_DISABLED);
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
                        callback.onLocation(location);
                        return;
                    }
                    requestLastKnownLocation(callback);
                })
                .addOnFailureListener(e -> requestLastKnownLocation(callback));
    }

    @SuppressLint("MissingPermission")
    private void requestLastKnownLocation(@NonNull Callback callback) {
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location);
                    } else {
                        callback.onError(Error.LOCATION_UNAVAILABLE);
                    }
                })
                .addOnFailureListener(e -> callback.onError(Error.LOCATION_UNAVAILABLE));
    }

    public void cancel() {
        if (currentLocationTokenSource != null) {
            currentLocationTokenSource.cancel();
            currentLocationTokenSource = null;
        }
    }
}

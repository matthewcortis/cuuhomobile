package com.example.cuutro.core.location;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.Style;
import com.trackasia.android.maps.TrackAsiaMap;

public class TrackAsiaMapController {

    public interface OnMapReady {
        void onMapReady(@NonNull TrackAsiaMap map);
    }

    private final MapView mapView;
    private TrackAsiaMap trackAsiaMap;

    public TrackAsiaMapController(@NonNull MapView mapView) {
        this.mapView = mapView;
    }

    public void onCreate(@Nullable Bundle savedState) {
        mapView.onCreate(savedState);
    }

    public void loadStyle(
            @NonNull String styleUrl,
            @Nullable LatLng initialTarget,
            double initialZoom,
            @Nullable OnMapReady onMapReady
    ) {
        mapView.getMapAsync(map -> {
            trackAsiaMap = map;
            map.setStyle(
                    new Style.Builder().fromUri(styleUrl),
                    style -> {
                        if (initialTarget != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialTarget, initialZoom));
                        }
                        if (onMapReady != null) {
                            onMapReady.onMapReady(map);
                        }
                    }
            );
        });
    }

    @Nullable
    public TrackAsiaMap getMap() {
        return trackAsiaMap;
    }

    public void onStart() {
        mapView.onStart();
    }

    public void onResume() {
        mapView.onResume();
    }

    public void onPause() {
        mapView.onPause();
    }

    public void onStop() {
        mapView.onStop();
    }

    public void onLowMemory() {
        mapView.onLowMemory();
    }

    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull String mapStateKey) {
        Bundle mapViewBundle = outState.getBundle(mapStateKey);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(mapStateKey, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }

    public void onDestroy() {
        mapView.onDestroy();
        trackAsiaMap = null;
    }
}

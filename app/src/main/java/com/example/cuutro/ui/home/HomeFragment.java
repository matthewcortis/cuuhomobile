package com.example.cuutro.ui.home;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cuutro.R;
import com.trackasia.android.TrackAsia;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.Style;
import com.trackasia.android.maps.TrackAsiaMap;

public class HomeFragment extends Fragment {

    private static final String MAP_VIEW_STATE_KEY = "home_map_view_state";
     private static final LatLng HANOI = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_ZOOM_VIETNAM = 9.4;

    private MapView mapView;
    private TrackAsiaMap trackAsiaMap;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TrackAsia.getInstance(requireContext().getApplicationContext());

        mapView = view.findViewById(R.id.home_map_view);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this::setupMap);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
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

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        trackAsiaMap = null;
        super.onDestroyView();
    }

    private void setupMap(@NonNull TrackAsiaMap map) {
        trackAsiaMap = map;
        map.setStyle(
                new Style.Builder().fromUri(getString(R.string.trackasia_style_url)),
                style -> map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(HANOI, DEFAULT_ZOOM_VIETNAM)
                )
        );
    }
}

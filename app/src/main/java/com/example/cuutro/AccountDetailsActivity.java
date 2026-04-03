package com.example.cuutro;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.trackasia.android.TrackAsia;
import com.trackasia.android.camera.CameraUpdateFactory;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;
import com.trackasia.android.maps.Style;

public class AccountDetailsActivity extends AppCompatActivity {

    private static final String MAP_VIEW_STATE_KEY = "account_map_view_state";
    private static final LatLng PUNE = new LatLng(18.5204, 73.8567);
    private static final double DEFAULT_ZOOM = 13.2;

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        TrackAsia.getInstance(getApplicationContext());

        mapView = findViewById(R.id.account_details_map_view);
        if (mapView != null) {
            Bundle mapViewBundle = null;
            if (savedInstanceState != null) {
                mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
            }
            mapView.onCreate(mapViewBundle);
            mapView.getMapAsync(map -> map.setStyle(
                    new Style.Builder().fromUri(getString(R.string.trackasia_style_url)),
                    style -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(PUNE, DEFAULT_ZOOM))
            ));
        }

        View backButton = findViewById(R.id.btn_account_details_back);
        View cancelButton = findViewById(R.id.btn_account_cancel);

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
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
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
}

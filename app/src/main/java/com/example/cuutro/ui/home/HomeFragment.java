package com.example.cuutro.ui.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String MAP_VIEW_STATE_KEY = "home_map_view_state";
     private static final LatLng HANOI = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_ZOOM_VIETNAM = 9.4;
    private static final long SOS_PULSE_DURATION_MS = 1800L;

    private MapView mapView;
    private TrackAsiaMap trackAsiaMap;
    private final List<ObjectAnimator> sosPulseAnimators = new ArrayList<>();

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

        initSosPulse(view);
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
        stopSosPulse();
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

    private void initSosPulse(@NonNull View root) {
        View pulse1 = root.findViewById(R.id.sos_pulse_1);
        View pulse2 = root.findViewById(R.id.sos_pulse_2);
        if (pulse1 == null || pulse2 == null) {
            return;
        }
        stopSosPulse();
        startPulseAnimator(pulse1, 0L);
        startPulseAnimator(pulse2, SOS_PULSE_DURATION_MS / 2);
    }

    private void startPulseAnimator(@NonNull View pulseView, long startDelay) {
        pulseView.setScaleX(1f);
        pulseView.setScaleY(1f);
        pulseView.setAlpha(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(pulseView, View.SCALE_X, 1f, 1.9f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(pulseView, View.SCALE_Y, 1f, 1.9f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(pulseView, View.ALPHA, 0.58f, 0f);

        configurePulseAnimator(scaleX, startDelay);
        configurePulseAnimator(scaleY, startDelay);
        configurePulseAnimator(alpha, startDelay);

        sosPulseAnimators.add(scaleX);
        sosPulseAnimators.add(scaleY);
        sosPulseAnimators.add(alpha);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void configurePulseAnimator(@NonNull ObjectAnimator animator, long startDelay) {
        animator.setDuration(SOS_PULSE_DURATION_MS);
        animator.setStartDelay(startDelay);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
    }

    private void stopSosPulse() {
        for (ObjectAnimator animator : sosPulseAnimators) {
            animator.cancel();
        }
        sosPulseAnimators.clear();
    }
}

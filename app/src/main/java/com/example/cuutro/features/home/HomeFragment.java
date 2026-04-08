package com.example.cuutro.features.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cuutro.R;
import com.example.cuutro.features.report.ReportActivity;
import com.example.cuutro.core.location.TrackAsiaMapController;
import com.trackasia.android.geometry.LatLng;
import com.trackasia.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String MAP_VIEW_STATE_KEY = "home_map_view_state";
    private static final LatLng HANOI = new LatLng(21.0285, 105.8542);
    private static final double DEFAULT_ZOOM_VIETNAM = 9.4;
    private static final long SOS_PULSE_DURATION_MS = 1800L;

    private MapView mapView;
    private TrackAsiaMapController mapController;
    private final List<ObjectAnimator> sosPulseAnimators = new ArrayList<>();

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.home_map_view);
        if (mapView != null) {
            mapController = new TrackAsiaMapController(mapView);
        }

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_STATE_KEY);
        }
        if (mapController != null) {
            mapController.onCreate(mapViewBundle);
            mapController.loadStyle(
                    getString(R.string.trackasia_style_url),
                    HANOI,
                    DEFAULT_ZOOM_VIETNAM,
                    null
            );
        }

        initSosPulse(view);
        setupSosAction(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapController != null) {
            mapController.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapController != null) {
            mapController.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapController != null) {
            mapController.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapController != null) {
            mapController.onSaveInstanceState(outState, MAP_VIEW_STATE_KEY);
        }
    }

    @Override
    public void onDestroyView() {
        stopSosPulse();
        if (mapController != null) {
            mapController.onDestroy();
            mapController = null;
        }
        mapView = null;
        super.onDestroyView();
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

    private void setupSosAction(@NonNull View root) {
        View sosButton = root.findViewById(R.id.btn_home_sos);
        if (sosButton == null) {
            return;
        }
        sosButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReportActivity.class))
        );
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

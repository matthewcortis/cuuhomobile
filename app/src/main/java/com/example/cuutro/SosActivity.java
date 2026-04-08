package com.example.cuutro;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cuutro.features.report.ReportActivity;

import java.util.ArrayList;
import java.util.List;

public class SosActivity extends AppCompatActivity {

    private static final long SOS_PULSE_DURATION_MS = 1800L;
    private static final float SOS_PULSE_MAX_SCALE = 1.55f;

    private final List<ObjectAnimator> sosPulseAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupSosButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initSosPulse();
    }

    @Override
    protected void onStop() {
        stopSosPulse();
        super.onStop();
    }

    private void setupSosButton() {
        View sosButton = findViewById(R.id.btn_sos);
        if (sosButton == null) {
            return;
        }
        sosButton.setOnClickListener(v ->
                startActivity(new Intent(SosActivity.this, ReportActivity.class))
        );
    }

    private void initSosPulse() {
        View pulse1 = findViewById(R.id.sos_pulse_1);
        View pulse2 = findViewById(R.id.sos_pulse_2);
        View pulse3 = findViewById(R.id.sos_pulse_3);
        if (pulse1 == null || pulse2 == null || pulse3 == null) {
            return;
        }

        stopSosPulse();
        startPulseAnimator(pulse1, 0L);
        startPulseAnimator(pulse2, SOS_PULSE_DURATION_MS / 3);
        startPulseAnimator(pulse3, (SOS_PULSE_DURATION_MS / 3) * 2);
    }

    private void startPulseAnimator(@NonNull View pulseView, long startDelay) {
        pulseView.setScaleX(1f);
        pulseView.setScaleY(1f);
        pulseView.setAlpha(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(pulseView, View.SCALE_X, 1f, SOS_PULSE_MAX_SCALE);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(pulseView, View.SCALE_Y, 1f, SOS_PULSE_MAX_SCALE);
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

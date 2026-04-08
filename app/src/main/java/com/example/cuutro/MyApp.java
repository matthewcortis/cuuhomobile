package com.example.cuutro;

import android.app.Application;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import com.trackasia.android.TrackAsia;

public class MyApp extends Application implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.6f;
    private static final int REQUIRED_SHAKE_COUNT = 2;
    private static final long SHAKE_WINDOW_MS = 550L;
    private static final long SOS_LAUNCH_COOLDOWN_MS = 2500L;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private int shakeCount;
    private long firstShakeTimestampMs;
    private long lastSosLaunchTimestampMs;

    @Override
    public void onCreate() {
        super.onCreate();
        TrackAsia.getInstance(this);
        setupShakeListener();
    }

    @Override
    public void onTerminate() {
        teardownShakeListener();
        super.onTerminate();
    }

    private void setupShakeListener() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        }
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor == null) {
            return;
        }
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void teardownShakeListener() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) {
            return;
        }
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce < SHAKE_THRESHOLD_GRAVITY) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (firstShakeTimestampMs == 0L || now - firstShakeTimestampMs > SHAKE_WINDOW_MS) {
            firstShakeTimestampMs = now;
            shakeCount = 1;
            return;
        }

        shakeCount += 1;
        if (shakeCount < REQUIRED_SHAKE_COUNT) {
            return;
        }

        resetShakeWindow();
        launchSosIfAllowed(now);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op.
    }

    private void resetShakeWindow() {
        firstShakeTimestampMs = 0L;
        shakeCount = 0;
    }

    private void launchSosIfAllowed(long now) {
        if (now - lastSosLaunchTimestampMs < SOS_LAUNCH_COOLDOWN_MS) {
            return;
        }
        lastSosLaunchTimestampMs = now;

        Intent intent = new Intent(this, SosActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}

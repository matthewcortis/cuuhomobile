package com.example.cuutro;

import android.app.Application;
import com.trackasia.android.TrackAsia;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TrackAsia.getInstance(this);
    }
}

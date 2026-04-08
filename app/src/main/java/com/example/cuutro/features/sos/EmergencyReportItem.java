package com.example.cuutro.features.sos;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class EmergencyReportItem {

    private final String id;
    private final String location;
    private final String title;
    private final String description;
    private final int iconResId;

    public EmergencyReportItem(
            @NonNull String id,
            @NonNull String location,
            @NonNull String title,
            @NonNull String description,
            @DrawableRes int iconResId
    ) {
        this.id = id;
        this.location = location;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getLocation() {
        return location;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }
}

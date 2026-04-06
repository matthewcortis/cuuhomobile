package com.example.cuutro.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;

import com.trackasia.android.geometry.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationAddressResolver {

    private final Context appContext;
    private final Locale locale;

    public LocationAddressResolver(@NonNull Context context, @NonNull Locale locale) {
        this.appContext = context.getApplicationContext();
        this.locale = locale;
    }

    @SuppressWarnings("deprecation")
    @NonNull
    public LocationAddressData reverseGeocode(@NonNull LatLng target, @NonNull String fallbackAddress) {
        if (!Geocoder.isPresent()) {
            return new LocationAddressData(fallbackAddress, fallbackAddress, "", "");
        }

        Geocoder geocoder = new Geocoder(appContext, locale);
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    target.getLatitude(),
                    target.getLongitude(),
                    1
            );

            if (addresses == null || addresses.isEmpty()) {
                return new LocationAddressData(fallbackAddress, fallbackAddress, "", "");
            }

            Address address = addresses.get(0);
            String fullAddress = buildFullAddress(address);
            String addressLine = firstNonBlank(
                    address.getSubLocality(),
                    address.getThoroughfare(),
                    address.getFeatureName(),
                    splitAddressSegment(fullAddress, 0)
            );
            String city = firstNonBlank(
                    address.getSubAdminArea(),
                    address.getLocality(),
                    address.getAdminArea(),
                    splitAddressSegment(fullAddress, 1)
            );
            String postalCode = firstNonBlank(
                    address.getPostalCode(),
                    splitAddressSegment(fullAddress, 2)
            );

            if (fullAddress.isBlank()) {
                fullAddress = fallbackAddress;
            }

            return new LocationAddressData(fullAddress, addressLine, city, postalCode);
        } catch (IOException | IllegalArgumentException ignored) {
            return new LocationAddressData(fallbackAddress, fallbackAddress, "", "");
        }
    }

    @NonNull
    private String buildFullAddress(Address address) {
        if (address == null) {
            return "";
        }

        if (address.getMaxAddressLineIndex() >= 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                String line = address.getAddressLine(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(line.trim());
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        return firstNonBlank(address.getFeatureName(), "");
    }

    @NonNull
    private String splitAddressSegment(String fullAddress, int index) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return "";
        }
        String[] parts = fullAddress.split(",");
        if (index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index].trim();
    }

    @NonNull
    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.trim().isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }
}

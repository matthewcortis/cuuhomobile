package com.example.cuutro.location;

import androidx.annotation.NonNull;

public class LocationAddressData {

    private final String fullAddress;
    private final String addressLine;
    private final String city;
    private final String postalCode;

    public LocationAddressData(
            @NonNull String fullAddress,
            @NonNull String addressLine,
            @NonNull String city,
            @NonNull String postalCode
    ) {
        this.fullAddress = fullAddress;
        this.addressLine = addressLine;
        this.city = city;
        this.postalCode = postalCode;
    }

    @NonNull
    public String getFullAddress() {
        return fullAddress;
    }

    @NonNull
    public String getAddressLine() {
        return addressLine;
    }

    @NonNull
    public String getCity() {
        return city;
    }

    @NonNull
    public String getPostalCode() {
        return postalCode;
    }
}

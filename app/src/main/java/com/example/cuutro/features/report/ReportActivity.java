package com.example.cuutro.features.report;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cuutro.R;
import com.example.cuutro.core.location.ReportLocationBottomSheet;
import com.google.android.material.button.MaterialButton;
import com.trackasia.android.geometry.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private static final LatLng DEFAULT_LOCATION = new LatLng(21.0285, 105.8542);

    private final List<View> emergencyItemViews = new ArrayList<>();

    private List<EmergencyType> emergencyTypes;
    private int selectedEmergencyIndex = 0;

    private TextView reportLocationValueText;
    private String selectedLocationText;
    private LatLng selectedLocationLatLng = DEFAULT_LOCATION;

    private ReportLocationBottomSheet reportLocationBottomSheet;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reportLocationValueText = findViewById(R.id.tvReportLocationValue);
        selectedLocationText = reportLocationValueText != null
                ? reportLocationValueText.getText().toString().trim()
                : getString(R.string.report_location_value);

        setupLocationPermissionLauncher();
        setupLocationBottomSheet();
        setupEmergencyTypeGrid();
        setupActions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        if (reportLocationBottomSheet != null) {
            reportLocationBottomSheet.onDestroy();
            reportLocationBottomSheet = null;
        }
        super.onDestroy();
    }

    private void setupLocationBottomSheet() {
        reportLocationBottomSheet = new ReportLocationBottomSheet(
                this,
                getString(R.string.trackasia_style_url),
                new ReportLocationBottomSheet.Listener() {
                    @Override
                    public boolean hasLocationPermission() {
                        return ReportActivity.this.hasLocationPermission();
                    }

                    @Override
                    public void requestLocationPermission() {
                        if (locationPermissionLauncher != null) {
                            locationPermissionLauncher.launch(new String[] {
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            });
                        }
                    }

                    @Override
                    public void onLocationConfirmed(String locationText, LatLng latLng) {
                        selectedLocationText = locationText;
                        if (latLng != null) {
                            selectedLocationLatLng = latLng;
                        }
                        if (reportLocationValueText != null) {
                            reportLocationValueText.setText(locationText);
                        }
                    }
                }
        );
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean hasFine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean hasCoarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    boolean granted = hasFine || hasCoarse;
                    if (reportLocationBottomSheet != null) {
                        reportLocationBottomSheet.onLocationPermissionResult(granted);
                    }
                }
        );
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupActions() {
        ImageButton backButton = findViewById(R.id.btnBack);
        MaterialButton submitButton = findViewById(R.id.btnSubmitReport);
        TextView changeLocationText = findViewById(R.id.tvChangeLocation);

        backButton.setOnClickListener(v -> finish());
        changeLocationText.setOnClickListener(v -> {
            if (reportLocationBottomSheet != null) {
                reportLocationBottomSheet.show(selectedLocationText, selectedLocationLatLng);
            }
        });
        submitButton.setOnClickListener(v -> {
            String selectedLabel = emergencyTypes.get(selectedEmergencyIndex).label;
            Toast.makeText(
                    this,
                    getString(R.string.report_selected_type, selectedLabel),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupEmergencyTypeGrid() {
        GridLayout gridLayout = findViewById(R.id.gridEmergencyTypes);
        emergencyTypes = buildEmergencyTypes();
        LayoutInflater inflater = LayoutInflater.from(this);

        gridLayout.removeAllViews();
        emergencyItemViews.clear();

        for (int i = 0; i < emergencyTypes.size(); i++) {
            EmergencyType emergencyType = emergencyTypes.get(i);
            View itemView = inflater.inflate(R.layout.item_emergency_type, gridLayout, false);

            EmergencyTypeViewHolder holder = new EmergencyTypeViewHolder(itemView);
            holder.iconView.setImageResource(emergencyType.iconResId);
            holder.nameView.setText(emergencyType.label);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
            );
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            int horizontalMargin = dpToPx(2);
            int verticalMargin = dpToPx(8);
            params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            itemView.setLayoutParams(params);

            int position = i;
            itemView.setOnClickListener(v -> selectEmergencyType(position));

            itemView.setTag(holder);
            emergencyItemViews.add(itemView);
            gridLayout.addView(itemView);
        }

        selectEmergencyType(selectedEmergencyIndex);
    }

    private List<EmergencyType> buildEmergencyTypes() {
        return Arrays.asList(
                new EmergencyType(getString(R.string.report_accident), R.drawable.ic_emergency_accident),
                new EmergencyType(getString(R.string.report_fire), R.drawable.ic_emergency_fire),
                new EmergencyType(getString(R.string.report_medical), R.drawable.ic_emergency_medical),
                new EmergencyType(getString(R.string.report_flood), R.drawable.ic_emergency_flood),
                new EmergencyType(getString(R.string.report_quake), R.drawable.ic_emergency_quake),
                new EmergencyType(getString(R.string.report_robbery), R.drawable.ic_emergency_robbery),
                new EmergencyType(getString(R.string.report_assault), R.drawable.ic_emergency_assault),
                new EmergencyType(getString(R.string.report_other), R.drawable.ic_emergency_other)
        );
    }

    private void selectEmergencyType(int position) {
        selectedEmergencyIndex = position;

        int selectedTextColor = ContextCompat.getColor(this, R.color.report_text_primary);
        int unselectedTextColor = ContextCompat.getColor(this, R.color.report_label_inactive);
        int selectedIconTint = ContextCompat.getColor(this, R.color.white);
        int unselectedIconTint = ContextCompat.getColor(this, R.color.report_icon_inactive);

        for (int i = 0; i < emergencyItemViews.size(); i++) {
            View itemView = emergencyItemViews.get(i);
            EmergencyTypeViewHolder holder = (EmergencyTypeViewHolder) itemView.getTag();
            boolean isSelected = i == position;

            holder.iconContainer.setBackgroundResource(
                    isSelected
                            ? R.drawable.bg_report_emergency_icon_selected
                            : R.drawable.bg_report_emergency_icon_unselected
            );
            holder.iconView.setImageTintList(
                    ColorStateList.valueOf(isSelected ? selectedIconTint : unselectedIconTint)
            );
            holder.nameView.setTextColor(isSelected ? selectedTextColor : unselectedTextColor);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class EmergencyType {
        final String label;
        final int iconResId;

        EmergencyType(String label, int iconResId) {
            this.label = label;
            this.iconResId = iconResId;
        }
    }

    private static class EmergencyTypeViewHolder {
        final FrameLayout iconContainer;
        final ImageView iconView;
        final TextView nameView;

        EmergencyTypeViewHolder(View itemView) {
            iconContainer = itemView.findViewById(R.id.flEmergencyIconContainer);
            iconView = itemView.findViewById(R.id.ivEmergencyIcon);
            nameView = itemView.findViewById(R.id.tvEmergencyName);
        }
    }
}

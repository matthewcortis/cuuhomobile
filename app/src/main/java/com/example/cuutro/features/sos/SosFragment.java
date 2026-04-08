package com.example.cuutro.features.sos;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.cuutro.R;

import java.util.ArrayList;
import java.util.List;

public class SosFragment extends Fragment {

    private final List<EmergencyReportItem> reportedByYou = new ArrayList<>();
    private final List<EmergencyReportItem> reportedByOthers = new ArrayList<>();

    private EmergencyReportAdapter adapter;
    private TextView reportedByYouView;
    private TextView reportedByOthersView;
    private TextView emptyStateView;

    private boolean isShowingReportedByYou = true;

    public SosFragment() {
        super(R.layout.fragment_sos);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reportedByYouView = view.findViewById(R.id.btnReportedByYou);
        reportedByOthersView = view.findViewById(R.id.btnReportedByOthers);
        emptyStateView = view.findViewById(R.id.tvEmergencyEmpty);

        setupRecyclerView(view);
        setupFilterActions();
        seedData();
        renderCurrentTab();
    }

    private void setupRecyclerView(@NonNull View root) {
        RecyclerView recyclerView = root.findViewById(R.id.rvEmergencyReports);
        adapter = new EmergencyReportAdapter((item, position) -> {
            List<EmergencyReportItem> activeList = isShowingReportedByYou
                    ? reportedByYou
                    : reportedByOthers;
            if (position < 0 || position >= activeList.size()) {
                return;
            }
            activeList.remove(position);
            Toast.makeText(requireContext(), R.string.sos_deleted_toast, Toast.LENGTH_SHORT).show();
            renderCurrentTab();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterActions() {
        if (reportedByYouView != null) {
            reportedByYouView.setOnClickListener(v -> {
                if (!isShowingReportedByYou) {
                    isShowingReportedByYou = true;
                    renderCurrentTab();
                }
            });
        }
        if (reportedByOthersView != null) {
            reportedByOthersView.setOnClickListener(v -> {
                if (isShowingReportedByYou) {
                    isShowingReportedByYou = false;
                    renderCurrentTab();
                }
            });
        }
    }

    private void seedData() {
        reportedByYou.clear();
        reportedByOthers.clear();

        reportedByYou.add(new EmergencyReportItem(
                "me_1",
                getString(R.string.sos_sample_location_1),
                getString(R.string.sos_sample_title_accident),
                getString(R.string.sos_sample_description_accident),
                R.drawable.ic_emergency_accident
        ));
        reportedByYou.add(new EmergencyReportItem(
                "me_2",
                getString(R.string.sos_sample_location_2),
                getString(R.string.sos_sample_title_flood),
                getString(R.string.sos_sample_description_flood),
                R.drawable.ic_emergency_flood
        ));

        reportedByOthers.add(new EmergencyReportItem(
                "other_1",
                getString(R.string.sos_sample_location_3),
                getString(R.string.sos_sample_title_medical),
                getString(R.string.sos_sample_description_medical),
                R.drawable.ic_emergency_medical
        ));
        reportedByOthers.add(new EmergencyReportItem(
                "other_2",
                getString(R.string.sos_sample_location_4),
                getString(R.string.sos_sample_title_fire),
                getString(R.string.sos_sample_description_fire),
                R.drawable.ic_emergency_fire
        ));
    }

    private void renderCurrentTab() {
        if (adapter == null) {
            return;
        }

        List<EmergencyReportItem> activeList = isShowingReportedByYou
                ? reportedByYou
                : reportedByOthers;

        adapter.setShowDeleteAction(isShowingReportedByYou);
        adapter.submitList(activeList);
        updateFilterUi();
        updateEmptyState(activeList.isEmpty());
    }

    private void updateFilterUi() {
        if (reportedByYouView == null || reportedByOthersView == null) {
            return;
        }
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.report_action_red);
        int unselectedColor = ContextCompat.getColor(requireContext(), R.color.report_label_inactive);

        if (isShowingReportedByYou) {
            reportedByYouView.setBackgroundResource(R.drawable.bg_sos_filter_selected);
            reportedByOthersView.setBackgroundResource(android.R.color.transparent);
            reportedByYouView.setTextColor(selectedColor);
            reportedByOthersView.setTextColor(unselectedColor);
        } else {
            reportedByYouView.setBackgroundResource(android.R.color.transparent);
            reportedByOthersView.setBackgroundResource(R.drawable.bg_sos_filter_selected);
            reportedByYouView.setTextColor(unselectedColor);
            reportedByOthersView.setTextColor(selectedColor);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyStateView == null) {
            return;
        }
        emptyStateView.setText(
                isShowingReportedByYou
                        ? R.string.sos_empty_reported_by_you
                        : R.string.sos_empty_reported_by_others
        );
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}

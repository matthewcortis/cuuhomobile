package com.example.cuutro.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cuutro.AccountDetailsActivity;
import com.example.cuutro.R;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View accountDetailsButton = view.findViewById(R.id.chi_tiet_tai_khoan_btn);
        if (accountDetailsButton != null) {
            accountDetailsButton.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), AccountDetailsActivity.class))
            );
        }
    }
}

package com.example.cuutro;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.cuutro.ui.home.CommunityFragment;
import com.example.cuutro.ui.home.HomeFragment;
import com.example.cuutro.ui.home.ProfileFragment;
import com.example.cuutro.ui.home.SosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_SOS = "tab_sos";
    private static final String TAG_COMMUNITY = "tab_community";
    private static final String TAG_PROFILE = "tab_profile";

    private BottomNavigationView bottomNavigationView;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trang_chu);

        bottomNavigationView = findViewById(R.id.bottom_nav);
        restoreActiveFragment();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchToTab(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (activeFragment == null) {
            switchToTab(bottomNavigationView.getSelectedItemId());
        }
    }

    private void switchToTab(int itemId) {
        String tag = getTagForItem(itemId);
        if (tag == null) {
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment targetFragment = fragmentManager.findFragmentByTag(tag);
        if (targetFragment == null) {
            targetFragment = createFragment(itemId);
        }
        if (targetFragment == null) {
            return;
        }

        if (activeFragment == targetFragment && targetFragment.isVisible()) {
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction().setReorderingAllowed(true);
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.trang_chu_fragment_container, targetFragment, tag);
        }
        if (activeFragment != null && activeFragment != targetFragment && activeFragment.isAdded()) {
            transaction.hide(activeFragment);
        }
        transaction.show(targetFragment);
        transaction.commit();

        activeFragment = targetFragment;
    }

    private void restoreActiveFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isVisible()) {
                activeFragment = fragment;
                return;
            }
        }
    }

    private String getTagForItem(int itemId) {
        if (itemId == R.id.nav_home) {
            return TAG_HOME;
        }
        if (itemId == R.id.nav_sos) {
            return TAG_SOS;
        }
        if (itemId == R.id.nav_community) {
            return TAG_COMMUNITY;
        }
        if (itemId == R.id.nav_profile) {
            return TAG_PROFILE;
        }
        return null;
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.nav_home) {
            return new HomeFragment();
        }
        if (itemId == R.id.nav_sos) {
            return new SosFragment();
        }
        if (itemId == R.id.nav_community) {
            return new CommunityFragment();
        }
        if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return null;
    }
}

package com.example.cuutro.features.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cuutro.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityFragment extends Fragment {

    private final List<CommunityPostItem> postItems = new ArrayList<>();
    private CommunityPostAdapter adapter;
    private RecyclerView recyclerView;
    private int generatedPostCounter = 0;

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView(view);
        seedData();
        renderPosts();
        setupCreatePostAction(view);
    }

    private void setupRecyclerView(@NonNull View root) {
        recyclerView = root.findViewById(R.id.rvCommunityPosts);
        adapter = new CommunityPostAdapter((item, position) ->
                Toast.makeText(requireContext(), R.string.community_post_action_toast, Toast.LENGTH_SHORT).show()
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void seedData() {
        postItems.clear();
        postItems.add(new CommunityPostItem(
                "community_1",
                R.drawable.community_post_avatar_joshua,
                getString(R.string.community_post_author_joshua),
                true,
                getString(R.string.community_post_location_tokyo),
                R.drawable.community_post_tokyo,
                getString(R.string.community_post_media_counter_1_3),
                getString(R.string.community_post_liked_by),
                getString(R.string.community_post_caption_joshua),
                getString(R.string.community_post_date_september_19)
        ));
        postItems.add(new CommunityPostItem(
                "community_2",
                R.drawable.community_post_avatar_joshua,
                getString(R.string.community_post_author_rajesh),
                false,
                getString(R.string.community_post_location_pune),
                R.drawable.community_post_meals_round,
                getString(R.string.community_post_media_counter_1_1),
                getString(R.string.community_post_liked_by_community),
                getString(R.string.community_post_caption_rajesh),
                getString(R.string.community_post_date_january_19)
        ));
        generatedPostCounter = 2;
    }

    private void setupCreatePostAction(@NonNull View root) {
        FloatingActionButton createPostButton = root.findViewById(R.id.fabCreateCommunityPost);
        createPostButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddNewCommunityActivity.class);
            startActivity(intent);
        });
    }

    private void addGeneratedPost() {
        generatedPostCounter++;
        String dynamicId = "community_dynamic_" + generatedPostCounter;
        String dynamicDate = new SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(new Date());
        int imageResId = generatedPostCounter % 2 == 0
                ? R.drawable.community_post_tokyo
                : R.drawable.community_post_meals_round;

        postItems.add(0, new CommunityPostItem(
                dynamicId,
                R.drawable.community_post_avatar_joshua,
                getString(R.string.community_author_you),
                false,
                getString(R.string.community_post_location_local),
                imageResId,
                getString(R.string.community_post_media_counter_1_1),
                getString(R.string.community_dynamic_liked_by_text),
                getString(R.string.community_dynamic_caption_text, generatedPostCounter),
                dynamicDate
        ));
        renderPosts();
        if (recyclerView != null) {
            recyclerView.scrollToPosition(0);
        }
        Toast.makeText(requireContext(), R.string.community_post_created_toast, Toast.LENGTH_SHORT).show();
    }

    private void renderPosts() {
        if (adapter == null) {
            return;
        }
        adapter.submitList(postItems);
    }
}

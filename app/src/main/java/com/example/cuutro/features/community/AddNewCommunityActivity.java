package com.example.cuutro.features.community;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cuutro.R;

public class AddNewCommunityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_new_community);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupActions();
    }

    private void setupActions() {
        ImageButton backButton = findViewById(R.id.btnBackCreatePost);
        View submitButton = findViewById(R.id.btnSubmitCommunityPost);
        TextView changeLocationView = findViewById(R.id.tvCommunityPostChangeLocation);

        backButton.setOnClickListener(v -> finish());
        changeLocationView.setOnClickListener(v ->
                Toast.makeText(this, R.string.report_location_selected, Toast.LENGTH_SHORT).show()
        );
        submitButton.setOnClickListener(v -> {
            Toast.makeText(this, R.string.community_post_created_toast, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

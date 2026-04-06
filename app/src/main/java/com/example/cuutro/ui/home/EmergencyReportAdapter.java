package com.example.cuutro.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cuutro.R;

import java.util.ArrayList;
import java.util.List;

public class EmergencyReportAdapter extends RecyclerView.Adapter<EmergencyReportAdapter.EmergencyReportViewHolder> {

    public interface Listener {
        void onDeleteClicked(@NonNull EmergencyReportItem item, int position);
    }

    private final List<EmergencyReportItem> items = new ArrayList<>();
    private final Listener listener;
    private boolean showDeleteAction = true;

    public EmergencyReportAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<EmergencyReportItem> reports) {
        items.clear();
        items.addAll(reports);
        notifyDataSetChanged();
    }

    public void setShowDeleteAction(boolean showDeleteAction) {
        this.showDeleteAction = showDeleteAction;
    }

    @NonNull
    @Override
    public EmergencyReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_emergency_report, parent, false);
        return new EmergencyReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmergencyReportViewHolder holder, int position) {
        EmergencyReportItem item = items.get(position);
        holder.bind(item, showDeleteAction);
        holder.deleteButton.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (!showDeleteAction || currentPosition == RecyclerView.NO_POSITION) {
                return;
            }
            listener.onDeleteClicked(items.get(currentPosition), currentPosition);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EmergencyReportViewHolder extends RecyclerView.ViewHolder {

        private final TextView locationTextView;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ImageView iconImageView;
        private final ImageButton deleteButton;

        EmergencyReportViewHolder(@NonNull View itemView) {
            super(itemView);
            locationTextView = itemView.findViewById(R.id.tvReportItemLocation);
            titleTextView = itemView.findViewById(R.id.tvReportItemTitle);
            descriptionTextView = itemView.findViewById(R.id.tvReportItemDescription);
            iconImageView = itemView.findViewById(R.id.ivReportItemTypeIcon);
            deleteButton = itemView.findViewById(R.id.btnDeleteReport);
        }

        void bind(@NonNull EmergencyReportItem item, boolean canDelete) {
            locationTextView.setText(item.getLocation());
            titleTextView.setText(item.getTitle());
            descriptionTextView.setText(item.getDescription());
            iconImageView.setImageResource(item.getIconResId());
            deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
        }
    }
}

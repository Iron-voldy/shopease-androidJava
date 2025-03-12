package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.R;
import com.example.shopease.models.QuickAction;

import java.util.List;

public class QuickActionsAdapter extends RecyclerView.Adapter<QuickActionsAdapter.ViewHolder> {

    private final List<QuickAction> quickActions;
    private final OnQuickActionClickListener clickListener;

    public interface OnQuickActionClickListener {
        void onQuickActionClicked(QuickAction action);
    }

    public QuickActionsAdapter(List<QuickAction> quickActions, OnQuickActionClickListener clickListener) {
        this.quickActions = quickActions;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickAction action = quickActions.get(position);
        holder.actionTitle.setText(action.getTitle());
        holder.actionIcon.setImageResource(action.getIconResId());

        holder.itemView.setOnClickListener(v -> clickListener.onQuickActionClicked(action));
    }

    @Override
    public int getItemCount() {
        return quickActions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView actionTitle;
        ImageView actionIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            actionTitle = itemView.findViewById(R.id.actionTitle);
            actionIcon = itemView.findViewById(R.id.actionIcon);
        }
    }
}

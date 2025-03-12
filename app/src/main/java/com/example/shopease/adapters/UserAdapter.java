package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.shopease.R;
import com.example.shopease.models.UserModel;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<UserModel> userList;
    private final UserActionListener listener;

    public interface UserActionListener {
        void onBlockUser(UserModel user);
        void onChangeRole(UserModel user);
        void onViewUserDetails(UserModel user);
    }

    public UserAdapter(List<UserModel> userList, UserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        // Set user details
        holder.nameTextView.setText(user.getFirstName() + " " + user.getLastName());
        holder.emailTextView.setText(user.getEmail());

        // Set role with appropriate capitalization
        String role = user.getRole();
        if (role != null && !role.isEmpty()) {
            role = role.substring(0, 1).toUpperCase() + role.substring(1);
        }
        holder.roleTextView.setText(role);

        // Set status
        holder.statusTextView.setText(user.isBlocked() ? "Blocked" : "Active");
        holder.statusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(
                user.isBlocked() ? android.R.color.holo_red_dark : android.R.color.holo_green_dark
        ));

        // Load profile picture
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfilePictureUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_users)
                    .error(R.drawable.ic_users)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_users);
        }

        // Set button text based on user status
        holder.blockButton.setText(user.isBlocked() ? "Unblock" : "Block");

        // Setup button click listeners
        holder.blockButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBlockUser(user);
            }
        });

        holder.changeRoleButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChangeRole(user);
            }
        });

        // Setup item click listener for viewing details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewUserDetails(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView;
        TextView emailTextView;
        TextView roleTextView;
        TextView statusTextView;
        Button blockButton;
        Button changeRoleButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            roleTextView = itemView.findViewById(R.id.roleTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            blockButton = itemView.findViewById(R.id.blockButton);
            changeRoleButton = itemView.findViewById(R.id.changeRoleButton);
        }
    }
}
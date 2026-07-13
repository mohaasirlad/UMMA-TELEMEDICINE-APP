package com.example.ummatelemedicineapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_onboarding, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.setOnboardingData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivOnboarding;
        private TextView tvTitle;
        private TextView tvDescription;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOnboarding = itemView.findViewById(R.id.ivOnboardingImage);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvDescription = itemView.findViewById(R.id.tvOnboardingDescription);
        }

        void setOnboardingData(OnboardingItem onboardingItem) {
            tvTitle.setText(onboardingItem.getTitle());
            tvDescription.setText(onboardingItem.getDescription());
            ivOnboarding.setImageResource(onboardingItem.getImage());
        }
    }
}
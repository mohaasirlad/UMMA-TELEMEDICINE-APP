package com.example.ummatelemedicineapp.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.models.ChatMessage;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> chatMessages;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> chatMessages, String currentUserId) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        holder.bind(message, currentUserId);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        MaterialCardView cardMessage;
        LinearLayout container;
        android.widget.ImageView ivStatus;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvChatMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            cardMessage = itemView.findViewById(R.id.cardChatMessage);
            container = (LinearLayout) itemView;
            ivStatus = itemView.findViewById(R.id.ivMessageStatus);
        }

        void bind(ChatMessage message, String currentUserId) {
            tvMessage.setText(message.getMessage());
            
            String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(message.getTimestamp()));
            tvTime.setText(timeStr);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardMessage.getLayoutParams();
            if (message.getSenderId().equals(currentUserId)) {
                container.setGravity(Gravity.END);
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_brand));
                tvMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                tvTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                params.setMargins(64, 0, 0, 0);

                ivStatus.setVisibility(View.VISIBLE);
                if ("read".equals(message.getStatus())) {
                    ivStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.light_cyan));
                } else {
                    ivStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.white));
                }
            } else {
                container.setGravity(Gravity.START);
                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                tvMessage.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.black));
                tvTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_gray));
                params.setMargins(0, 0, 64, 0);
                ivStatus.setVisibility(View.GONE);
            }
            cardMessage.setLayoutParams(params);
        }
    }
}
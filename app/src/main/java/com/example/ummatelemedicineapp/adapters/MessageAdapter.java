package com.example.ummatelemedicineapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    private List<Message> messagesDisplayed;
    private List<Message> messagesFull;
    private OnMessageClickListener listener;
    private String currentQuery = "";

    public MessageAdapter(List<Message> messages, OnMessageClickListener listener) {
        this.messagesFull = new ArrayList<>(messages);
        this.messagesDisplayed = new ArrayList<>(messages);
        this.listener = listener;
    }

    public void updateList(List<Message> newList) {
        this.messagesFull = new ArrayList<>(newList);
        applyFilter(currentQuery);
    }

    public void filter(String text) {
        this.currentQuery = text;
        applyFilter(text);
    }

    private void applyFilter(String text) {
        messagesDisplayed = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            messagesDisplayed.addAll(messagesFull);
        } else {
            String query = text.toLowerCase().trim();
            for (Message item : messagesFull) {
                if (item.getSenderName().toLowerCase().contains(query) ||
                        item.getLastMessage().toLowerCase().contains(query)) {
                    messagesDisplayed.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_message, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messagesDisplayed.get(position);
        holder.setMessageData(message);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesDisplayed.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvName, tvLastMsg, tvTime, tvUnread;
        private View cardUnread;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivSenderProfile);
            tvName = itemView.findViewById(R.id.tvSenderName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
            tvUnread = itemView.findViewById(R.id.tvUnreadCount);
            cardUnread = itemView.findViewById(R.id.cardUnreadCount);
        }

        void setMessageData(Message message) {
            tvName.setText(message.getSenderName());
            tvLastMsg.setText(message.getLastMessage());
            tvTime.setText(message.getTime());
            ivProfile.setImageResource(message.getProfileImage());
            
            if (message.getUnreadCount() > 0) {
                cardUnread.setVisibility(View.VISIBLE);
                tvUnread.setText(String.valueOf(message.getUnreadCount()));
            } else {
                cardUnread.setVisibility(View.GONE);
            }
        }
    }
}

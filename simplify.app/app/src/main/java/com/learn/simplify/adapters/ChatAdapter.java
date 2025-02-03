package com.learn.simplify.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.learn.simplify.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        if (messages != null) {
            this.messages = messages;
        } else {
            this.messages = new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage chatMessage = messages.get(position);
        holder.bind(chatMessage);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(String sender, String message) {
        messages.add(new ChatMessage(sender, message));
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView textMessage;
        private TextView senderTextView;
        private View viewChatIndicator;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.promptChat);
            senderTextView = itemView.findViewById(R.id.sender);
            viewChatIndicator = itemView.findViewById(R.id.viewChatIndicator);
        }

        public void bind(ChatMessage chatMessage) {
            textMessage.setText(chatMessage.getMessage());
            senderTextView.setText(chatMessage.getSender());
            int grayColor = itemView.getContext().getResources().getColor(R.color.gray);
            int greenColor = itemView.getContext().getResources().getColor(R.color.green);

            if (chatMessage.getSender().equals("ME")) {
                senderTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.gray));
                viewChatIndicator.setBackgroundTintList(ColorStateList.valueOf(grayColor));
            } else {
                senderTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.green));
                viewChatIndicator.setBackgroundTintList(ColorStateList.valueOf(greenColor));
            }
        }
    }

    public static class ChatMessage {
        private String sender;
        private String message;

        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }
    }
}

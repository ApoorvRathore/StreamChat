package com.example.streamchat.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.streamchat.databinding.LayoutSentMessageBinding;
import com.example.streamchat.databinding.ReceivedMessagesBinding;
import com.example.streamchat.modals.ChatMessages;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public ChatAdapter(List<ChatMessages> chatMessages, String senderId, Bitmap receiverProfileImage) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.receiverProfileImage = receiverProfileImage;
    }

    private final List<ChatMessages> chatMessages;
    private final String senderId;
    private Bitmap receiverProfileImage;

    public final int VIEW_TYPE_SENT = 1;
    public final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    LayoutSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceiverMessageViewHolder(
                    ReceivedMessagesBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final LayoutSentMessageBinding binding;

        SentMessageViewHolder(LayoutSentMessageBinding messageBinding) {
            super(messageBinding.getRoot());
            binding = messageBinding;

        }

        void setData(ChatMessages chatMessages) {
            binding.txtMessage.setText(chatMessages.message);
            binding.txtDateTime.setText(chatMessages.dateTime);
        }
    }

    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ReceivedMessagesBinding binding;

        ReceiverMessageViewHolder(ReceivedMessagesBinding receivedMessagesBinding) {
            super(receivedMessagesBinding.getRoot());
            binding = receivedMessagesBinding;
        }

        void setData(ChatMessages chatMessages, Bitmap receiverProfileImage) {
            binding.txtMessage.setText(chatMessages.message);
            binding.txtDateTime.setText(chatMessages.dateTime);
            if (receiverProfileImage != null) {
                binding.imgProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}

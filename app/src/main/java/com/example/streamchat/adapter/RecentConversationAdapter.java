package com.example.streamchat.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.streamchat.R;
import com.example.streamchat.databinding.ItemContainRecentConversationBinding;
import com.example.streamchat.listener.ConversionListener;
import com.example.streamchat.modals.ChatMessages;
import com.example.streamchat.modals.Users;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder> {

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private final List<ChatMessages> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversationAdapter(List<ChatMessages> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainRecentConversationBinding binding;

        ConversionViewHolder(ItemContainRecentConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setData(ChatMessages chatMessages) {
            binding.imgProfile.setImageBitmap(getConversationImage(chatMessages.ConversionImage));
            binding.txtName.setText(chatMessages.conversionName);
            binding.txtRecentMessage.setText(chatMessages.message);
            binding.getRoot().setOnClickListener(view -> {
                Users user = new Users();
                user.id = chatMessages.conversionId;
                user.name = chatMessages.conversionName;
                user.image = chatMessages.ConversionImage;
                conversionListener.onConversionClicked(user);
            });
        }

        private Bitmap getConversationImage(String encodedImage) {
            if (encodedImage != null && !encodedImage.isEmpty()) {
                byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else {
                // Return a default bitmap if the image is null or empty
                return BitmapFactory.decodeResource(binding.getRoot().getResources(), R.drawable.default_profile);
            }
        }
    }
}

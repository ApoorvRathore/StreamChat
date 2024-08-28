package com.example.streamchat.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.streamchat.databinding.ItemContainUsersBinding;
import com.example.streamchat.listener.UserListener;
import com.example.streamchat.modals.Users;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    public UserAdapter(List<Users> users ,UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    private final List<Users> users;
    private final UserListener userListener;

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainUsersBinding itemContainUsersBinding = ItemContainUsersBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder((itemContainUsersBinding));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserDetails(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder{
       ItemContainUsersBinding binding;
        UserViewHolder(ItemContainUsersBinding itemContainUsersBinding){
            super(itemContainUsersBinding.getRoot());
            binding = itemContainUsersBinding;
        }
        void setUserDetails(Users users){
            binding.txtName.setText(users.name);
            binding.txtEmail.setText(users.email);
            binding.imgProfile.setImageBitmap(getUserImage(users.image));
            binding.getRoot().setOnClickListener(view -> userListener.onUserClicked(users));
        }
    }
    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }
}

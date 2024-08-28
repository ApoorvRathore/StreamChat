package com.example.streamchat.firebase;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.streamchat.activities.ChatActivity;
import com.example.streamchat.modals.Users;
import com.example.streamchat.utills.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Token: " + token);
        // Save or send the token to your server as needed
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Users users = new Users();
        users.id = message.getData().get(Constants.KEY_USER_ID);
        users.name = message.getData().get(Constants.KEY_NAME);
        users.token = message.getData().get(Constants.KEY_FCM_TOKEN);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER, users);








    }
}

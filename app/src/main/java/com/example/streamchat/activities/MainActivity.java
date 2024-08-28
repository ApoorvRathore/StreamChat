package com.example.streamchat.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.streamchat.R;
import com.example.streamchat.adapter.RecentConversationAdapter;
import com.example.streamchat.databinding.ActivityMainBinding;
import com.example.streamchat.listener.ConversionListener;
import com.example.streamchat.modals.ChatMessages;
import com.example.streamchat.modals.Users;
import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.EncryptionUtil;
import com.example.streamchat.utills.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKey;

public class MainActivity extends BaseActivity implements ConversionListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessages> conversations;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initialize();
        loadUserDetails();
        setListeners();
        try {
            listenToConversations();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        fetchToken();
    }

    private void initialize() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this);
        binding.conversationRecyclerView.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imgSignout.setOnClickListener(view -> signOut());
        binding.addUser.setOnClickListener(view -> navigateTo(UsersActivity.class));
        binding.imgProfile.setOnClickListener(view -> navigateTo(ProfileActivity.class));
    }

    private void navigateTo(Class<?> activityClass) {
        startActivity(new Intent(getApplicationContext(), activityClass));
    }

    private void loadUserDetails() {
        String userName = preferenceManager.getString(Constants.KEY_NAME);
        binding.txtName.setText(userName != null && !userName.isEmpty() ? userName : "Unknown User");

        String encodedImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (encodedImage != null && !encodedImage.isEmpty()) {
            displayProfileImage(encodedImage);
        } else {
            binding.imgProfile.setImageResource(R.drawable.default_profile);
        }
    }

    private void displayProfileImage(String encodedImage) {
        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imgProfile.setImageBitmap(bitmap);
        } catch (IllegalArgumentException e) {
            binding.imgProfile.setImageResource(R.drawable.default_profile);
            showToast("Invalid image format");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenToConversations() throws Exception {
        SecretKey key = EncryptionUtil.generateKey("your_secure_password"); // Replace with your actual key/password
        EventListener<QuerySnapshot> eventListener = (value, error) -> {
            if (error != null) {
                showToast("Error fetching conversations: " + error.getMessage());
                return;
            }
            if (value != null) {
                handleDocumentChanges(value, key);
            }
        };

        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                .addSnapshotListener(eventListener);
    }

    private void handleDocumentChanges(QuerySnapshot value, SecretKey key) {
        try {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                ChatMessages chatMessages = createChatMessage(documentChange, key);
                if (chatMessages != null) {
                    updateConversationList(chatMessages, documentChange);
                }
            }
            refreshConversationList();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error decrypting messages");
        }
    }

    private ChatMessages createChatMessage(DocumentChange documentChange, SecretKey key) {
        ChatMessages chatMessages = new ChatMessages();
        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
        chatMessages.senderId = senderId;
        chatMessages.receiverId = receiverId;

        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
            chatMessages.ConversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
            chatMessages.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
            chatMessages.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
        } else {
            chatMessages.ConversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
            chatMessages.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
            chatMessages.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
        }

        String encryptedMessage = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
        try {
            chatMessages.message = EncryptionUtil.decrypt(encryptedMessage, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        chatMessages.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);

        return chatMessages;
    }

    private void updateConversationList(ChatMessages chatMessages, DocumentChange documentChange) {
        if (documentChange.getType() == DocumentChange.Type.ADDED) {
            conversations.add(chatMessages);
        } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
            for (int i = 0; i < conversations.size(); i++) {
                if (conversations.get(i).senderId.equals(chatMessages.senderId) &&
                        conversations.get(i).receiverId.equals(chatMessages.receiverId)) {
                    conversations.get(i).message = chatMessages.message;
                    conversations.get(i).dateObject = chatMessages.dateObject;
                    break;
                }
            }
        }
    }

    private void refreshConversationList() {
        Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
        conversationAdapter.notifyDataSetChanged();
        binding.conversationRecyclerView.smoothScrollToPosition(0);
        binding.conversationRecyclerView.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }

    private void fetchToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken)
                .addOnFailureListener(e -> showToast("Failed to fetch token: " + e.getMessage()));
    }

    private void updateToken(String token) {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
            documentReference.update(Constants.KEY_FCM_TOKEN, token)
                    .addOnFailureListener(e -> showToast("Failed to update token: " + e.getMessage()));
        } else {
            showToast("User ID is not available, unable to update token");
        }
    }

    private void signOut() {
        showToast("Signing out...");
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
            HashMap<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        preferenceManager.clear();
                        navigateTo(SigninActivity.class);
                        finish();
                    })
                    .addOnFailureListener(e -> showToast("Unable to sign out: " + e.getMessage()));
        } else {
            showToast("User ID is not available, unable to sign out");
            preferenceManager.clear();
            navigateTo(SigninActivity.class);
            finish();
        }
    }

    @Override
    public void onConversionClicked(Users user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}

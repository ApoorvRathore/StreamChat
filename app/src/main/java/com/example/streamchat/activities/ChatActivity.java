package com.example.streamchat.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.streamchat.R;
import com.example.streamchat.adapter.ChatAdapter;
import com.example.streamchat.databinding.ActivityChatBinding;
import com.example.streamchat.modals.ChatMessages;
import com.example.streamchat.modals.Users;
import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.EncryptionUtil;
import com.example.streamchat.utills.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.SecretKey;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private Users receiverUser;
    private PreferenceManager preferenceManager;
    private ChatAdapter chatAdapter;
    private List<ChatMessages> chatMessages;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize necessary components
        loadReceiverDetail();
        setListener();
        init();
        listenMessage();
    }

    private void loadReceiverDetail() {
        receiverUser = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser != null) {
            binding.txtName.setText(receiverUser.name);
        } else {
            // Handle case where receiverUser is null
            finish();
        }
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID),
                getBitmapEncodedString(receiverUser.image)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }


    private Bitmap getBitmapEncodedString(@Nullable String encodedImage) {
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
        }
    }

    private void listenMessage() {
        // Listen for messages sent and received
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }


//    private void sendMessage() {
//        HashMap<String, Object> message = new HashMap<>();
//        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
//        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
//        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
//        message.put(Constants.KEY_TIMESTAMP, new Date());
//        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
//
//        if (conversionId != null) {
//            updateConversion(binding.inputMessage.getText().toString());
//        } else {
//            HashMap<String, Object> conversion = new HashMap<>();
//            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
//            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
//            conversion.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
//            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
//            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
//            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
//            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
//            conversion.put(Constants.KEY_TIMESTAMP, new Date());
//            addConversion(conversion);
//        }
//
//
//        binding.inputMessage.setText(null);
//    }
private void sendMessage() {
    try {
        SecretKey key = EncryptionUtil.generateKey("your_secure_password");
        String message = binding.inputMessage.getText().toString();
        String encryptedMessage = EncryptionUtil.encrypt(message, key);

        HashMap<String, Object> messageMap = new HashMap<>();
        messageMap.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        messageMap.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        messageMap.put(Constants.KEY_MESSAGE, encryptedMessage);
        messageMap.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(messageMap);

        // Handle conversation update
        if (conversionId != null) {
            updateConversion(encryptedMessage);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, encryptedMessage);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        binding.inputMessage.setText(null);
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(receiverUser.id)
                .addSnapshotListener(ChatActivity.this, ((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                            int availability = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                            isReceiverAvailable = availability == 1;
                        }
                        receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                        if (receiverUser.image != null) {
                            receiverUser.image = value.getString(Constants.KEY_IMAGE);
                            chatAdapter.setReceiverProfileImage(getBitmapEncodedString(receiverUser.image));
                            chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                        }
                    }
                    if (isReceiverAvailable) {
                        binding.txtAvailability.setVisibility(View.VISIBLE);
                    } else {
                        binding.txtAvailability.setVisibility(View.GONE);
                    }
                }));
    }

//    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
//        if (error != null) {
//            return;
//        }
//        if (value != null) {
//            int count = chatMessages.size();
//            for (DocumentChange documentChange : value.getDocumentChanges()) {
//                ChatMessages chatMessage = new ChatMessages();
//                if (documentChange.getType() == DocumentChange.Type.ADDED) {
//                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
//                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
//                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
//                    chatMessage.dateTime = getReadableDate(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
//                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
//                    chatMessages.add(chatMessage);
//                }
//            }
//            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
//            if (count == 0) {
//                chatAdapter.notifyDataSetChanged();
//            } else {
//                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
//                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
//            }
//            binding.chatRecyclerView.setVisibility(View.VISIBLE);
//        }
//        binding.progressBar.setVisibility(View.GONE);
//        if (conversionId == null) {
//            checkForConversion();
//        }
//    };
private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
    if (error != null) {
        return;
    }
    if (value != null) {
        int count = chatMessages.size();
        for (DocumentChange documentChange : value.getDocumentChanges()) {
            ChatMessages chatMessage = new ChatMessages();
            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                try {
                    SecretKey key = EncryptionUtil.generateKey("your_secure_password");
                    String encryptedMessage = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    String decryptedMessage = EncryptionUtil.decrypt(encryptedMessage, key);

                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = decryptedMessage;
                    chatMessage.dateTime = getReadableDate(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
        if (count == 0) {
            chatAdapter.notifyDataSetChanged();
        } else {
            chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
            binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        }
        binding.chatRecyclerView.setVisibility(View.VISIBLE);
    }
    binding.progressBar.setVisibility(View.GONE);
    if (conversionId == null) {
        checkForConversion();
    }
};

    private void setListener() {
        binding.imgBack.setOnClickListener(view -> onBackPressed());
        binding.btnSend.setOnClickListener(view -> sendMessage());
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id);
            checkConversionRemotely(receiverUser.id, preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    private void checkConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    private String getReadableDate(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}

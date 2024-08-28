package com.example.streamchat.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;
    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        final FirebaseFirestore database = FirebaseFirestore.getInstance();
        final String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (null != userId && !userId.isEmpty()) {
            this.documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        } else {
            Log.e(BaseActivity.TAG, "User ID is null or empty. Unable to create document reference.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.updateAvailabilityStatus(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.updateAvailabilityStatus(1);
    }

    private void updateAvailabilityStatus(final int status) {
        if (null != documentReference) {
            documentReference.update(Constants.KEY_AVAILABILITY, status)
                    .addOnSuccessListener(aVoid -> Log.d(BaseActivity.TAG, "User availability updated to " + status))
                    .addOnFailureListener(e -> Log.e(BaseActivity.TAG, "Failed to update availability: " + e.getMessage()));
        } else {
            Log.e(BaseActivity.TAG, "Document reference is null. Cannot update availability.");
        }
    }
}

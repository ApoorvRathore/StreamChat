package com.example.streamchat.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.R;
import com.example.streamchat.databinding.ActivityProfileBinding;
import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.PreferenceManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        assert imageUri != null;
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imgProfile.setImageBitmap(bitmap);
                        encodeImageAndSaveToFirebase(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();

        loadUserDetails();
        fetchImage();
        fetchUserData();
        clickListener();
    }

    private void fetchImage() {
        String encodedImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (encodedImage != null && !encodedImage.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.imgProfile.setImageBitmap(bitmap);
            } catch (Exception e) {
                binding.imgProfile.setImageResource(R.drawable.default_profile); // Default image
                Toast.makeText(getApplicationContext(), "Invalid Image Format", Toast.LENGTH_SHORT).show();
            }
        } else {
            binding.imgProfile.setImageResource(R.drawable.default_profile); // Default image
        }
    }

    private void fetchUserData() {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString(Constants.KEY_NAME);
                        String email = documentSnapshot.getString(Constants.KEY_EMAIL);
                        String phone = documentSnapshot.getString(Constants.KEY_PHONE);

                        binding.profileUsername.setText(name);
                        binding.profileEmail.setText(email);
                        binding.profilePhone.setText(phone);
                    } else {
                        showToast("User data not found");
                    }
                })
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }

    private void loadUserDetails() {
        String name = getIntent().getStringExtra(Constants.KEY_NAME);
        String email = getIntent().getStringExtra(Constants.KEY_EMAIL);
        String phone = getIntent().getStringExtra(Constants.KEY_PHONE);

        binding.profileUsername.setText(name);
        binding.profileEmail.setText(email);
        binding.profilePhone.setText(phone);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clickListener() {
        binding.btnEnableEdit.setOnClickListener(v -> enableEditing());
        binding.btnUpdate.setOnClickListener(v -> {
            updateValueInFirebase();
            updateValueInRealtime();
        });
        binding.imgProfile.setOnClickListener(v -> openImageChooser());
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.imgSignout.setOnClickListener(v -> signOut());
    }

    private void enableEditing() {
        binding.profileUsername.setEnabled(true);
        binding.profileUsername.setFocusable(true);
        binding.profileUsername.setFocusableInTouchMode(true);
        binding.profileUsername.requestFocus();
        // email
        binding.profileEmail.setEnabled(true);
        binding.profileEmail.setFocusable(true);
        binding.profileEmail.setFocusableInTouchMode(true);
        // phone
        binding.profilePhone.setEnabled(true);
        binding.profilePhone.setFocusable(true);
        binding.profilePhone.setFocusableInTouchMode(true);
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnUpdate.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnUpdate.setVisibility(View.VISIBLE);
        }
    }

    private void updateValueInRealtime() {
        setInProgress(true);
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        String newName = binding.profileUsername.getText().toString().trim();
        String newEmail = binding.profileEmail.getText().toString().trim();
        String newPhone = binding.profilePhone.getText().toString().trim();
        if (!newName.isEmpty() && !newEmail.isEmpty() && !newPhone.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_NAME, newName);
            updates.put(Constants.KEY_EMAIL, newEmail);
            updates.put(Constants.KEY_PHONE, newPhone);

            database.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> setInProgress(false))
                    .addOnFailureListener(e -> setInProgress(false));
        } else {
            setInProgress(false);
            showToast("Fields cannot be empty");
        }
    }

    private void updateValueInFirebase() {
        setInProgress(true);
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        String newName = binding.profileUsername.getText().toString().trim();
        String newEmail = binding.profileEmail.getText().toString().trim();
        String newPhone = binding.profilePhone.getText().toString().trim();
        if (!newName.isEmpty() && !newEmail.isEmpty() && !newPhone.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_NAME, newName);
            updates.put(Constants.KEY_EMAIL, newEmail);
            updates.put(Constants.KEY_PHONE, newPhone);

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constants.KEY_COLLECTION_USERS);
            databaseReference.child(userId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        setInProgress(false);
                        showToast("Value updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        setInProgress(false);
                        showToast("Failed to update value: " + e.getMessage());
                    });
        } else {
            setInProgress(false);
            showToast("Fields cannot be empty");
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void encodeImageAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_IMAGE, encodedImage);

        setInProgress(true);
        database.collection(Constants.KEY_COLLECTION_USERS).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    setInProgress(false);
                    showToast("Image updated successfully");
                })
                .addOnFailureListener(e -> {
                    setInProgress(false);
                    showToast("Failed to update image: " + e.getMessage());
                });
    }

    private void signOut() {
        showToast("Signing out...");

        // Retrieve user ID from preferences
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null && !userId.isEmpty()) {
            // Proceed with sign-out if user ID is valid
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);

            HashMap<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());

            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        preferenceManager.clear();
                        startActivity(new Intent(getApplicationContext(), SigninActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> showToast("Unable to sign out: " + e.getMessage()));
        } else {
            // Handle the case where user ID is null or empty
            showToast("User ID is not available, unable to sign out");
            // Optionally clear preferences and redirect to sign-in screen
            preferenceManager.clear();
            startActivity(new Intent(getApplicationContext(), SigninActivity.class));
            finish();
        }
    }
}


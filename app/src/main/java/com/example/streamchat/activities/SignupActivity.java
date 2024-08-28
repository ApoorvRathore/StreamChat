package com.example.streamchat.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.databinding.ActivitySignupBinding;
import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    DatabaseReference databasereference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        databasereference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://streamchat-ac9d5-default-rtdb.firebaseio.com/");

        setListener();
    }

    private void setListener() {
        binding.txtSignin.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), SigninActivity.class)));
        binding.layoutImage.setOnClickListener(view -> {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    pickImage.launch(intent);
                }
        );
        binding.btnSignup.setOnClickListener(view -> {
            if (isValidSignUp()) {
                signup();
                addRealtimeData();
            }

        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signup() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        RadioButton radioButton = findViewById(binding.radioGroup.getCheckedRadioButtonId());
        String gender = radioButton.getText().toString();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PHONE, binding.inputPhone.getText().toString());
        user.put(Constants.KEY_PASSWORD, hashPassword(binding.inputPassword.getText().toString()));
        user.put(Constants.KEY_GENDER, gender);
        user.put(Constants.KEY_IMAGE, encodedImage);
        user.put(Constants.KEY_USER_ID, FirebaseAuth.getInstance().getUid());
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private Boolean isValidSignUp() {
        if (encodedImage == null) {
            showToast("Please Select your profile picture");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Please Enter your name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Please enter your email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Please enter valid mail");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Please enter your password");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Please enter confirm password");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Password not match");
            return false;
        } else if (binding.radioGroup.getCheckedRadioButtonId() == -1) {
            showToast("Please select gender");
            return false;
        } else {
            return true;
        }
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            assert imageUri != null;
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imgProfile.setImageBitmap(bitmap);
                            binding.txtAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.btnSignup.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.btnSignup.setVisibility(View.VISIBLE);
        }
    }

    private void addRealtimeData() {
        String gender;
        String name = binding.inputName.getText().toString();
        String email = binding.inputEmail.getText().toString();
        String phone = binding.inputPhone.getText().toString();
        String password = binding.inputPassword.getText().toString();
        if (binding.male.isChecked()) {
            gender = "Male";
        } else {
            gender = "Female";
        }
        if (TextUtils.isEmpty(name)) {
            binding.inputName.setError("Please Enter Name");
        } else if (TextUtils.isEmpty(email)) {
            binding.inputEmail.setError("Please enter Email");
        } else if (TextUtils.isEmpty(phone)) {
            binding.inputPhone.setError("Please enter phone no");
        } else if (binding.inputPhone.length() < 10) {
            binding.inputPhone.setError("Please Enter 10 digit number");
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.setError("Invalid Mail");
        } else if (password.length() < 8) {
            binding.inputPassword.setError("Password Should contain at-least 8 character");
        } else {

            databasereference.child("user").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild(phone)) {
                        showToast("Mobile number already exist");
                        binding.inputPhone.setError("Mobile number already exist");
                    } else {
                        databasereference.child("user").child(phone).child("Name").setValue(name);
                        databasereference.child("user").child(phone).child("Phone No").setValue(phone);
                        databasereference.child("user").child(phone).child("Email").setValue(email);
                        databasereference.child("user").child(phone).child("Password").setValue(hashPassword(password));
                        databasereference.child("user").child(phone).child("Gender").setValue(gender);
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] salt = getSalt();
            digest.update(salt);
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Method to generate a salt
    private byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }
}
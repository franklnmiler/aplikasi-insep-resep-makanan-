package com.example.project111;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// ... (package & imports tetap sama)

public class ProfileActivity extends AppCompatActivity {

    private static final String SUPABASE_STORAGE_URL = "https://zwyjincljcwqjyagzwci.supabase.co/storage/v1/object";
    private static final String SUPABASE_BUCKET = "project111";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp3eWppbmNsamN3cWp5YWd6d2NpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTQyMTM5MSwiZXhwIjoyMDYwOTk3MzkxfQ.974CVHyzfxfqloQxLUg8AVZJT_gNWYPGjrSHOA2FqSQ"; // Jangan hardcode di produksi

    private ImageView profileImage;
    private EditText editUsername, editPassword, editPhone;
    private TextView textEmail;
    private Button btnSave, btnLogout, btnHelp, btnRequest;
    private Uri imageUri;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private DatabaseReference databaseReference;
    private OkHttpClient httpClient = new OkHttpClient();
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private BottomNavigationView bottomNavigationView;
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        initializeFirebase();
        setupImagePickerLauncher();
        setupButtonActions();
        setupMenuButton();
        setupBottomNavigation();
        loadUserData();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        editPhone = findViewById(R.id.edit_phone);
        textEmail = findViewById(R.id.text_email);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);
        btnHelp = findViewById(R.id.btn_help);
        btnRequest = findViewById(R.id.btn_request);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_menu) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(ProfileActivity.this, TemanActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        userId = sharedPreferences.getString("userId", "");
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    editUsername.setText(snapshot.child("username").getValue(String.class));
                    editPassword.setText(snapshot.child("password").getValue(String.class));
                    editPhone.setText(snapshot.child("nomorHp").getValue(String.class));
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                    if (email != null) {
                        textEmail.setText(email);
                        editor.putString("email", email); // Simpan ke SharedPreferences
                        editor.apply();
                    }

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(profileImageUrl).into(profileImage);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Data user tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtonActions() {
        profileImage.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnHelp.setOnClickListener(v -> openWhatsApp("Halo, saya butuh bantuan."));
        btnRequest.setOnClickListener(v -> openWhatsApp("Halo, ini info device saya: " + getDeviceInfo()));
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        profileImage.setImageURI(imageUri);
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        saveTextData();
        if (imageUri != null) {
            uploadImageAndSaveProfile();
        } else {
            String existingImageUrl = sharedPreferences.getString("profileImage", "");
            saveUserData(existingImageUrl);
            Toast.makeText(this, "Profil disimpan tanpa ubah foto.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTextData() {
        editor.putString("username", editUsername.getText().toString().trim());
        editor.putString("password", editPassword.getText().toString().trim());
        editor.putString("nomorHp", editPhone.getText().toString().trim());
        editor.apply();
    }

    private void uploadImageAndSaveProfile() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengunggah gambar...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) throw new IOException("Gagal membuka file.");

            String fileExtension = getFileExtension(imageUri);
            File tempFile = File.createTempFile("upload_", "." + fileExtension, getCacheDir());

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            String filename = UUID.randomUUID().toString() + "." + fileExtension;
            MediaType mediaType = MediaType.parse(getContentResolver().getType(imageUri));
            if (mediaType == null) mediaType = MediaType.parse("image/*");

            RequestBody fileBody = RequestBody.create(tempFile, mediaType);

            Request request = new Request.Builder()
                    .url(SUPABASE_STORAGE_URL + "/" + SUPABASE_BUCKET + "/" + filename)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .put(fileBody)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    progressDialog.dismiss();
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Gagal upload gambar.", Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_STORAGE_URL + "/public/" + SUPABASE_BUCKET + "/" + filename;
                        editor.putString("profileImage", imageUrl);
                        editor.apply();
                        saveUserData(imageUrl);
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Profil berhasil diperbarui.", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Upload gagal: " + response.message(), Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (IOException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error saat upload gambar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveUserData(String imageUrl) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("username", editUsername.getText().toString().trim());
        userMap.put("password", editPassword.getText().toString().trim());
        userMap.put("nomorHp", editPhone.getText().toString().trim());
        userMap.put("profileImage", imageUrl);
        userMap.put("device_info", getDeviceInfo());

        String existingEmail = sharedPreferences.getString("email", "");
        if (!existingEmail.isEmpty()) {
            userMap.put("email", existingEmail);  // Pastikan email tetap disimpan
        }

        databaseReference.child(userId).setValue(userMap);
    }

    private void logout() {
        SharedPreferences preferences = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(ProfileActivity.this, login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openWhatsApp(String message) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp tidak terinstall.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileExtension(Uri uri) {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
    }

    private String getDeviceInfo() {
        return "Model: " + Build.MODEL + ", Brand: " + Build.BRAND + ", Android: " + Build.VERSION.RELEASE;
    }

    private void setupMenuButton() {
        Button itemVerification = findViewById(R.id.itemVerification);
        Button itemRiwayat = findViewById(R.id.itemRiwayat);
        Button itemFavorit = findViewById(R.id.itemFavorit);
        Button itemBantuan = findViewById(R.id.itemBantuan);

        itemVerification.setOnClickListener(v -> startActivity(new Intent(this, VerifikasiActivity.class)));
        itemRiwayat.setOnClickListener(v -> startActivity(new Intent(this, RiwayatActivity.class)));
        itemFavorit.setOnClickListener(v -> startActivity(new Intent(this, FavoriteRecipesActivity.class)));
        itemBantuan.setOnClickListener(v -> startActivity(new Intent(this, BantuanActivity.class)));
    }
}

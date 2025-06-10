package com.example.project111;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.*;

public class AddRecipeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etName, etDescription, etIngredients, etSteps, etVideoUrl;
    private Spinner spinnerCategory;
    private ImageView imagePreview;
    private Uri imageUri;
    private Button btnChooseImage, btnSave;

    private static final String SUPABASE_STORAGE_URL = "https://zwyjincljcwqjyagzwci.supabase.co/storage/v1/object";
    private static final String SUPABASE_BUCKET = "project111";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp3eWppbmNsamN3cWp5YWd6d2NpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTQyMTM5MSwiZXhwIjoyMDYwOTk3MzkxfQ.974CVHyzfxfqloQxLUg8AVZJT_gNWYPGjrSHOA2FqSQ"; // Ganti dengan API key asli

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        databaseReference = FirebaseDatabase.getInstance().getReference("recipes");

        initializeViews();
        setupCategorySpinner();
        setupButtonListeners();
    }

    private void initializeViews() {
        etName = findViewById(R.id.et_recipe_name);
        etDescription = findViewById(R.id.et_description);
        etIngredients = findViewById(R.id.et_ingredients);
        etSteps = findViewById(R.id.et_steps);
        etVideoUrl = findViewById(R.id.et_video_url);
        spinnerCategory = findViewById(R.id.spinner_category);
        imagePreview = findViewById(R.id.image_preview);
        btnChooseImage = findViewById(R.id.btn_choose_image);
        btnSave = findViewById(R.id.btn_save_recipe);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.recipe_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> validateAndUploadRecipe());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
        }
    }

    private void validateAndUploadRecipe() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String ingredients = etIngredients.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();
        String videoUrl = etVideoUrl.getText().toString().trim();

        List<String> ingredientsList = Arrays.asList(ingredients.split("\\s*,\\s*"));
        List<String> stepsList = Arrays.asList(steps.split("\\s*\\n\\s*"));

        if (name.isEmpty() || description.isEmpty() || ingredientsList.isEmpty() || stepsList.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Lengkapi semua data dan pilih gambar!", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImageToSupabase(name, description, category, ingredientsList, stepsList, videoUrl);
    }

    private void uploadImageToSupabase(String name, String description, String category,
                                       List<String> ingredientsList, List<String> stepsList, String videoUrl) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Mengompres & mengunggah gambar...");
        pd.setCancelable(false);
        pd.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) throw new IOException("Gagal membaca file gambar.");

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) throw new IOException("Gagal decode gambar.");

            // Ukur ukuran awal
            // Ukur ukuran awal
            ByteArrayOutputStream testStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, testStream);
            int originalSizeMB = testStream.toByteArray().length / (1024 * 1024);

// Tentukan kualitas kompresi
            int quality;
            if (originalSizeMB >= 8 && originalSizeMB <= 15) {
                quality = 40; // untuk target ~4MB
            } else if (originalSizeMB >= 4 && originalSizeMB <= 7) {
                quality = 60; // untuk target ~2MB
            } else {
                quality = 85; // kompres ringan jika kecil
            }


            // Simpan hasil kompres ke file sementara
            File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }

            String filename = UUID.randomUUID().toString() + ".jpg";
            MediaType mediaType = MediaType.parse("image/jpeg");

            RequestBody fileBody = RequestBody.create(tempFile, mediaType);

            Request uploadRequest = new Request.Builder()
                    .url(SUPABASE_STORAGE_URL + "/" + SUPABASE_BUCKET + "/" + filename)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .put(fileBody)
                    .build();

            new OkHttpClient().newCall(uploadRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    pd.dismiss();
                    runOnUiThread(() -> Toast.makeText(AddRecipeActivity.this, "Gagal upload gambar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    pd.dismiss();
                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_STORAGE_URL.replace("/storage/v1/object", "") +
                                "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + filename;
                        runOnUiThread(() -> saveRecipeToFirebase(name, description, category, ingredientsList, stepsList, videoUrl, imageUrl));
                    } else {
                        runOnUiThread(() -> Toast.makeText(AddRecipeActivity.this, "Gagal unggah gambar: " + response.message(), Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (IOException e) {
            pd.dismiss();
            Toast.makeText(this, "Kesalahan saat membaca/mengompres gambar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveRecipeToFirebase(String name, String description, String category,
                                      List<String> ingredientsList, List<String> stepsList,
                                      String videoUrl, String imageUrl) {

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menyimpan resep ke Firebase...");
        pd.setCancelable(false);
        pd.show();

        String recipeId = databaseReference.push().getKey();
        if (recipeId == null) {
            pd.dismiss();
            Toast.makeText(this, "Gagal membuat ID resep.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "Unknown User");

        Recipe recipe = new Recipe(
                recipeId,
                name,
                description,
                imageUrl,
                category,
                ingredientsList,
                stepsList,
                videoUrl,
                username,
                System.currentTimeMillis()
        );

        databaseReference.child(recipeId).setValue(recipe)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Resep berhasil disimpan!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Gagal menyimpan resep.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getFileExtension(Uri uri) {
        if (uri.getScheme().equals("content")) {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
        } else {
            return MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
    }
}

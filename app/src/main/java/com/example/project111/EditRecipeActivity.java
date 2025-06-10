package com.example.project111;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.*;

public class EditRecipeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String SUPABASE_STORAGE_URL = "https://zwyjincljcwqjyagzwci.supabase.co/storage/v1/object";
    private static final String SUPABASE_BUCKET = "project111";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp3eWppbmNsamN3cWp5YWd6d2NpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0NTQyMTM5MSwiZXhwIjoyMDYwOTk3MzkxfQ.974CVHyzfxfqloQxLUg8AVZJT_gNWYPGjrSHOA2FqSQ";

    private EditText etName, etDescription, etIngredients, etSteps, etVideoUrl;
    private Spinner spinnerCategory;
    private ImageView imagePreview;
    private Uri imageUri;
    private Button btnChooseImage, btnSave;

    private DatabaseReference recipesRef;
    private Recipe recipeToEdit;
    private String existingImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        initializeViews();
        setupCategorySpinner();
        loadRecipeData();
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
        btnChooseImage = findViewById(R.id.btn_ganti_image);
        btnSave = findViewById(R.id.btn_save_edit);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.recipe_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void loadRecipeData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("recipe")) {
            recipeToEdit = (Recipe) intent.getSerializableExtra("recipe");
            if (recipeToEdit != null) {
                etName.setText(recipeToEdit.getName());
                etDescription.setText(recipeToEdit.getDescription());
                etIngredients.setText(String.join(", ", recipeToEdit.getIngredients()));
                etSteps.setText(String.join("\n", recipeToEdit.getSteps()));
                etVideoUrl.setText(recipeToEdit.getVideoUrl());
                existingImageUrl = recipeToEdit.getImageUrl();
                // Set spinner category
                String[] categories = getResources().getStringArray(R.array.recipe_categories);
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(recipeToEdit.getCategory())) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
                // Tampilkan gambar existing (bisa pakai Glide/Picasso kalau mau)
                // Contoh pakai Glide:
                // Glide.with(this).load(existingImageUrl).into(imagePreview);
            }
        }
    }

    private void setupButtonListeners() {
        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> validateAndUpdateRecipe());
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

    private void validateAndUpdateRecipe() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String ingredientsText = etIngredients.getText().toString().trim();
        String stepsText = etSteps.getText().toString().trim();
        String videoUrl = etVideoUrl.getText().toString().trim();

        List<String> ingredientsList = Arrays.asList(ingredientsText.split("\\s*,\\s*"));
        List<String> stepsList = Arrays.asList(stepsText.split("\\s*\\n\\s*"));

        if (name.isEmpty() || description.isEmpty() || ingredientsList.isEmpty() || stepsList.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data resep!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageAndSave(name, description, category, ingredientsList, stepsList, videoUrl);
        } else {
            saveRecipeToFirebase(name, description, category, ingredientsList, stepsList, videoUrl, existingImageUrl);
        }
    }

    private void uploadImageAndSave(String name, String description, String category,
                                    List<String> ingredientsList, List<String> stepsList, String videoUrl) {

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Mengunggah gambar...");
        pd.setCancelable(false);
        pd.show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) throw new IOException("Gagal membaca file gambar.");

            String fileExt = getFileExtension(imageUri);
            File tempFile = File.createTempFile("upload_", "." + fileExt, getCacheDir());

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            String filename = UUID.randomUUID().toString() + "." + fileExt;
            MediaType mediaType = MediaType.parse(getContentResolver().getType(imageUri));
            if (mediaType == null) mediaType = MediaType.parse("image/*");

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
                    runOnUiThread(() -> Toast.makeText(EditRecipeActivity.this, "Gagal upload gambar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    pd.dismiss();
                    if (response.isSuccessful()) {
                        String imageUrl = SUPABASE_STORAGE_URL.replace("/storage/v1/object", "") +
                                "/storage/v1/object/public/" + SUPABASE_BUCKET + "/" + filename;
                        runOnUiThread(() -> saveRecipeToFirebase(name, description, category, ingredientsList, stepsList, videoUrl, imageUrl));
                    } else {
                        runOnUiThread(() -> Toast.makeText(EditRecipeActivity.this, "Gagal unggah gambar: " + response.message(), Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (IOException e) {
            pd.dismiss();
            Toast.makeText(this, "Kesalahan saat membaca gambar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveRecipeToFirebase(String name, String description, String category,
                                      List<String> ingredientsList, List<String> stepsList,
                                      String videoUrl, String imageUrl) {

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Menyimpan perubahan...");
        pd.setCancelable(false);
        pd.show();

        if (recipeToEdit == null) {
            pd.dismiss();
            Toast.makeText(this, "Resep tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "Unknown User");

        recipeToEdit.setName(name);
        recipeToEdit.setDescription(description);
        recipeToEdit.setCategory(category);
        recipeToEdit.setIngredients(ingredientsList);
        recipeToEdit.setSteps(stepsList);
        recipeToEdit.setVideoUrl(videoUrl);
        recipeToEdit.setImageUrl(imageUrl);
        recipeToEdit.setAuthor(username);


        recipesRef.child(recipeToEdit.getId()).setValue(recipeToEdit)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Resep berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Gagal memperbarui resep.", Toast.LENGTH_SHORT).show();
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

package com.example.project111;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DetailRecipeActivity extends AppCompatActivity {

    private TextView tvAuthor, tvCategory, tvDescription, tvIngredients, tvSteps, tvFavoriteCount, tvVideoUrl;
    private ImageView imgRecipe;
    private RecyclerView recyclerViewComments;
    private EditText editTextComment;
    private Button btnAddComment, btnShare;
    private FloatingActionButton fabFavorite;

    private CommentAdapter adapter;
    private List<Comment> commentList;
    private CommentManager commentManager;
    private Recipe recipe;

    private int favoriteCount = 0;
    private boolean isFavorite = false;

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_recipe);

        recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe == null) {
            Toast.makeText(this, "Data resep tidak ditemukan!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userManager = new UserManager(this);

        // Inisialisasi view
        tvAuthor = findViewById(R.id.tv_author);
        tvCategory = findViewById(R.id.tv_category);
        tvDescription = findViewById(R.id.tv_description);
        tvIngredients = findViewById(R.id.tv_ingredients_list1);
        tvSteps = findViewById(R.id.tv_steps_list);
        tvFavoriteCount = findViewById(R.id.tv_favorite_count);
        tvVideoUrl = findViewById(R.id.detail_video_url);

        imgRecipe = findViewById(R.id.img_recipe);
        recyclerViewComments = findViewById(R.id.recycler_view_comments);
        editTextComment = findViewById(R.id.edit_text_comment);
        btnAddComment = findViewById(R.id.btn_add_comment);
        btnShare = findViewById(R.id.btn_share);
        fabFavorite = findViewById(R.id.fab_favorite);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentManager = new CommentManager(this, recipe.getId());
        adapter = new CommentAdapter(this, commentList, commentManager);
        recyclerViewComments.setAdapter(adapter);

        showRecipeDetails();
        loadComments();
        loadFavoriteStatus();

        // Tambah komentar
        btnAddComment.setOnClickListener(v -> {
            String commentText = editTextComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                commentManager.addComment(commentText, new CommentManager.OnCompleteListener() {
                    @Override
                    public void onComplete() {
                        editTextComment.setText("");
                        loadComments();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(DetailRecipeActivity.this, "Gagal menambahkan komentar", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // Tombol favorit
        fabFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteStatus();
        });

        // Tombol bagikan
        btnShare.setOnClickListener(v -> {
            String shareText = "Resep oleh " + recipe.getAuthor() + ":\n\n" + recipe.getDescription() +
                    "\n\nLihat selengkapnya: " + recipe.getVideoUrl();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Bagikan resep via"));
        });
    }

    private void showRecipeDetails() {
        tvAuthor.setText("Author: " + recipe.getAuthor());
        tvCategory.setText("Kategori: " + recipe.getCategory());
        tvDescription.setText(recipe.getDescription());
        tvIngredients.setText("- " + String.join("\n- ", recipe.getIngredients()));
        tvSteps.setText(formatSteps(recipe.getSteps()));
        tvVideoUrl.setText(recipe.getVideoUrl());

        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(imgRecipe);
    }

    private String formatSteps(List<String> steps) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            builder.append((i + 1)).append(". ").append(steps.get(i)).append("\n");
        }
        return builder.toString().trim();
    }

    private void loadComments() {
        commentManager.loadComments(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot commentSnapshot : snapshot.getChildren()) {
                    Comment comment = commentSnapshot.getValue(Comment.class);
                    if (comment != null) {
                        comment.commentId = commentSnapshot.getKey();
                        comment.replies = new HashMap<>();

                        DataSnapshot repliesSnapshot = commentSnapshot.child("replies");
                        for (DataSnapshot replySnap : repliesSnapshot.getChildren()) {
                            Reply reply = replySnap.getValue(Reply.class);
                            comment.replies.put(replySnap.getKey(), reply);
                        }

                        commentList.add(comment);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Error handling
            }
        });
    }

    private void loadFavoriteStatus() {
        String userId = userManager.getUserId();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(recipe.getId());

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                favoriteCount = (int) snapshot.getChildrenCount();
                isFavorite = snapshot.hasChild(userId);

                tvFavoriteCount.setText(String.valueOf(favoriteCount));
                updateFavoriteIcon();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void updateFavoriteStatus() {
        String userId = userManager.getUserId();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("favorites").child(recipe.getId());
        DatabaseReference recipeRef = FirebaseDatabase.getInstance().getReference("recipes").child(recipe.getId());

        if (isFavorite) {
            favoritesRef.child(userId).setValue(true);
            favoriteCount++;
        } else {
            favoritesRef.child(userId).removeValue();
            favoriteCount = Math.max(0, favoriteCount - 1);
        }

        tvFavoriteCount.setText(String.valueOf(favoriteCount));
        recipeRef.child("favoriteCount").setValue(favoriteCount);
        updateFavoriteIcon();
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            fabFavorite.setImageResource(R.drawable.ic_favorit); // aktif
        } else {
            fabFavorite.setImageResource(R.drawable.ic_favorit); // non-aktif
        }
    }
}

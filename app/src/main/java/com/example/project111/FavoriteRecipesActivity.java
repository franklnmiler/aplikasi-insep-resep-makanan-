package com.example.project111;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoriteRecipesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> favoriteRecipeList;
    private UserManager userManager;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_recipes);

        recyclerViewFavorites = findViewById(R.id.recycler_view_favorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));

        favoriteRecipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(this, favoriteRecipeList);
        recyclerViewFavorites.setAdapter(recipeAdapter);

        userManager = new UserManager(this);
        userId = userManager.getUserId();

        // Menambahkan listener untuk klik item
        recipeAdapter.setOnItemClickListener(recipe -> {
            // Ketika item diklik, buka activity detail dengan ID resep
            Intent intent = new Intent(FavoriteRecipesActivity.this, DetailRecipeActivity.class);
            intent.putExtra("recipe", recipe);  // Kirim objek recipe ke DetailActivity
            startActivity(intent);
        });

        loadFavoriteRecipes();
    }

    private void loadFavoriteRecipes() {
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("favorites");
        DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference("recipes");

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> favoriteRecipeIds = new ArrayList<>();

                // Mendapatkan ID resep favorit berdasarkan user
                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    if (recipeSnapshot.hasChild(userId)) {
                        favoriteRecipeIds.add(recipeSnapshot.getKey());
                    }
                }

                if (favoriteRecipeIds.isEmpty()) {
                    Toast.makeText(FavoriteRecipesActivity.this, "Belum ada resep favorit", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Memuat resep-resep favorit berdasarkan ID
                for (String recipeId : favoriteRecipeIds) {
                    recipesRef.child(recipeId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot recipeSnapshot) {
                            Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(recipeSnapshot.getKey());
                                favoriteRecipeList.add(recipe);
                                recipeAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Menangani kesalahan jika ada
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Menangani kesalahan jika ada
            }
        });
    }
}

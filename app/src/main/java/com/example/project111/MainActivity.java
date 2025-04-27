package com.example.project111;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import android.view.View;


import java.util.ArrayList; import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();
    private List<Recipe> fullRecipeList = new ArrayList<>();
    private DatabaseReference databaseRef;

    private EditText searchBar;
    private Button camilanButton, kueButton, masakanButton, kulinerButton, minumanButton;
    private ImageView profileSmallIcon, friendSearchIcon, notificationIcon;
    private TextView usernameText, notificationBadge;

    private Button activeCategoryButton = null;
    private String activeCategory = null;
    private TextWatcher searchWatcher;

    private int currentRecipeCount = 0;
    private boolean initialLoadDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        setupRecyclerView();
        loadUserProfile();
        loadDataFromFirebase();
        setupSearchBar();
        setupCategoryButtons();
        setupFriendSearch();
        setupBottomNavigation();
        setupNotificationListener();
    }

    private void initUI() {
        recyclerView = findViewById(R.id.recyclerView);
        searchBar = findViewById(R.id.search_bar);
        camilanButton = findViewById(R.id.camilanButton);
        kueButton = findViewById(R.id.KueButton);
        masakanButton = findViewById(R.id.MasakanButton);
        kulinerButton = findViewById(R.id.KulinerButton);
        minumanButton = findViewById(R.id.MinumanButton);
        profileSmallIcon = findViewById(R.id.profileSmallIcon);
        usernameText = findViewById(R.id.usernameText);
        friendSearchIcon = findViewById(R.id.friendSearchIcon);
        notificationIcon = findViewById(R.id.notificationIcon);
        notificationBadge = findViewById(R.id.notificationBadge);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new RecipeAdapter(this, recipeList, false);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(recipe -> {
            Intent intent = new Intent(MainActivity.this, DetailRecipeActivity.class);
            intent.putExtra("recipe", recipe);
            startActivity(intent);
        });

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_menu);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_menu) {
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(MainActivity.this, TemanActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        String username = prefs.getString("username", null);

        if (username == null) {
            usernameText.setText("Halo, Guest");
            profileSmallIcon.setImageResource(R.drawable.ic_eye);
            return;
        }

        usernameText.setText("Halo, " + username);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_favorit)
                                .error(R.drawable.ic_eye)
                                .circleCrop()
                                .into(profileSmallIcon);
                    } else {
                        profileSmallIcon.setImageResource(R.drawable.ic_favorit);
                    }

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("profileImage", profileImageUrl != null ? profileImageUrl : "");
                    editor.apply();
                } else {
                    profileSmallIcon.setImageResource(R.drawable.ic_eye);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Gagal memuat profil", Toast.LENGTH_SHORT).show();
            }
        });

        profileSmallIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadDataFromFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference("recipes");

        databaseRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeList.clear();
                fullRecipeList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Recipe recipe = dataSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        recipeList.add(recipe);
                        fullRecipeList.add(recipe);
                    }
                }

                recipeList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                fullRecipeList.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

                adapter.updateList(new ArrayList<>(recipeList));

                recyclerView.postDelayed(() -> {
                    LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(MainActivity.this, R.anim.layout_animation_fall_down);
                    recyclerView.setLayoutAnimation(animation);
                    recyclerView.scheduleLayoutAnimation();
                    adapter.notifyDataSetChanged();
                }, 100);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Gagal memuat data resep", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchBar() {
        searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    resetFilter();
                } else {
                    filterRecipesByName(query);
                }
            }
        };
        searchBar.addTextChangedListener(searchWatcher);
    }

    private void setupCategoryButtons() {
        camilanButton.setOnClickListener(v -> handleCategoryClick(camilanButton, "Camilan"));
        kueButton.setOnClickListener(v -> handleCategoryClick(kueButton, "Kue"));
        masakanButton.setOnClickListener(v -> handleCategoryClick(masakanButton, "Masakan"));
        kulinerButton.setOnClickListener(v -> handleCategoryClick(kulinerButton, "Kuliner"));
        minumanButton.setOnClickListener(v -> handleCategoryClick(minumanButton, "Minuman"));
    }

    private void handleCategoryClick(Button button, String category) {
        if (activeCategory != null && activeCategory.equalsIgnoreCase(category)) {
            resetFilter();
            searchBar.removeTextChangedListener(searchWatcher);
            searchBar.setText("");
            searchBar.addTextChangedListener(searchWatcher);
        } else {
            filterByCategory(button, category);
            searchBar.removeTextChangedListener(searchWatcher);
            searchBar.setText("");
            searchBar.addTextChangedListener(searchWatcher);
        }
    }

    private void filterRecipesByName(String query) {
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : fullRecipeList) {
            if (recipe.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(recipe);
            }
        }
        adapter.updateList(filtered);
        clearActiveCategory();
    }

    private void filterByCategory(Button button, String category) {
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : fullRecipeList) {
            if (recipe.getCategory().equalsIgnoreCase(category)) {
                filtered.add(recipe);
            }
        }
        adapter.updateList(filtered);
        setActiveCategory(button, category);
    }

    private void resetFilter() {
        adapter.updateList(new ArrayList<>(fullRecipeList));
        clearActiveCategory();
    }

    private void setActiveCategory(Button button, String category) {
        if (activeCategoryButton != null) {
            activeCategoryButton.setBackgroundColor(getResources().getColor(R.color.default_category_color));
        }
        activeCategoryButton = button;
        activeCategory = category;
        activeCategoryButton.setBackgroundColor(getResources().getColor(R.color.teal_200));
    }

    private void clearActiveCategory() {
        if (activeCategoryButton != null) {
            activeCategoryButton.setBackgroundColor(getResources().getColor(R.color.default_category_color));
            activeCategoryButton = null;
        }
        activeCategory = null;
    }

    private void setupFriendSearch() {
        friendSearchIcon.setOnClickListener(v -> {
            // Intent untuk membuka ChatListActivity
            Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
            startActivity(intent);
        });
    }


    private void setupNotificationListener() {
        databaseRef = FirebaseDatabase.getInstance().getReference("recipes");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int newCount = (int) snapshot.getChildrenCount();

                if (initialLoadDone) {
                    if (newCount > currentRecipeCount) {
                        int newItems = newCount - currentRecipeCount;
                        showNotificationBadge(newItems);
                    }
                } else {
                    initialLoadDone = true;
                }

                currentRecipeCount = newCount;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Gagal memantau resep baru", Toast.LENGTH_SHORT).show();
            }
        });

        notificationIcon.setOnClickListener(v -> {
            hideNotificationBadge();
            recyclerView.smoothScrollToPosition(0);
        });
    }

    private void showNotificationBadge(int count) {
        notificationBadge.setVisibility(View.VISIBLE);
        notificationBadge.setText(String.valueOf(count));
    }

    private void hideNotificationBadge() {
        notificationBadge.setVisibility(View.GONE);
        notificationBadge.setText("0");
    }
}
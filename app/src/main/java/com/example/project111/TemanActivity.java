package com.example.project111;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class TemanActivity extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private EditText searchEditText;
    private List<User> userList, filteredList;
    private UserAdapter userAdapter;
    private DatabaseReference databaseRef;
    private ImageView profileSmallIcon;
    private TextView usernameText;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView; // ✅ fix deklarasi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teman);

        // Inisialisasi komponen UI
        mAuth = FirebaseAuth.getInstance();
        searchEditText = findViewById(R.id.searchEditText);
        userRecyclerView = findViewById(R.id.userRecyclerView);
        profileSmallIcon = findViewById(R.id.profileSmallIcon);
        usernameText = findViewById(R.id.usernameText);
        bottomNavigationView = findViewById(R.id.bottomNavigationView); // ✅ jangan double deklarasi

        // Set item yang aktif di bottom nav
        bottomNavigationView.setSelectedItemId(R.id.nav_search);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_menu) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0); // opsional tanpa animasi
                return true;
            } else if (id == R.id.nav_search) {
                return true; // sudah di sini
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Setup RecyclerView
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        userAdapter = new UserAdapter(this, filteredList);
        userRecyclerView.setAdapter(userAdapter);

        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        loadUserProfile();
        loadUsers();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        userAdapter.setOnUserClickListener(user -> {
            Intent intent = new Intent(TemanActivity.this, UserProfileActivity.class);
            intent.putExtra("username", user.getUsername());
            intent.putExtra("profileImage", user.getProfileImage());
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = databaseRef.child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImage").getValue(String.class);

                    usernameText.setText(username != null ? "Halo, " + username : "Halo, pengguna");

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(TemanActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_favorit)
                                .error(R.drawable.ic_eye)
                                .circleCrop()
                                .into(profileSmallIcon);
                    } else {
                        profileSmallIcon.setImageResource(R.drawable.ic_favorit);
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TemanActivity.this, "Gagal memuat profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null) userList.add(user);
                }
                filterUsers(searchEditText.getText().toString());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TemanActivity.this, "Gagal memuat daftar teman", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String keyword) {
        filteredList.clear();
        for (User user : userList) {
            if (user.getUsername() != null && user.getUsername().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }
}

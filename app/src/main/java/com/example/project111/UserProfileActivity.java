package com.example.project111;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView followersCountTextView;
    private TextView followingCountTextView;
    private TextView postCountTextView;
    private Button followButton;
    private Button messageButton;

    private RecyclerView recyclerViewHistory;
    private RecipeAdapter historyAdapter;
    private List<Recipe> historyList;

    private DatabaseReference followRef;
    private DatabaseReference recipesRef;
    private DatabaseReference usersRef;

    private String profileUsername;
    private boolean isFollowing = false;
    private String currentUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Ambil user login saat ini dari FirebaseAuth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserUid = firebaseUser.getUid(); // Ambil UID dari FirebaseAuth
        } else {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inisialisasi UI
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        followersCountTextView = findViewById(R.id.followersCountTextView);
        followingCountTextView = findViewById(R.id.followingCountTextView);
        postCountTextView = findViewById(R.id.postCountTextView);
        followButton = findViewById(R.id.followButton);
        messageButton = findViewById(R.id.messageButton);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);

        // RecyclerView setup
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        historyAdapter = new RecipeAdapter(this, historyList, true);
        recyclerViewHistory.setAdapter(historyAdapter);

        // Firebase reference
        followRef = FirebaseDatabase.getInstance().getReference("follows");
        recipesRef = FirebaseDatabase.getInstance().getReference("recipes");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Ambil username dan foto dari Intent
        profileUsername = getIntent().getStringExtra("username");

        if (profileUsername == null) {
            // Jika tidak ada, fallback ke SharedPreferences (misalnya, untuk diri sendiri)
            SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
            profileUsername = prefs.getString("username", "Unknown");
        }

        String profileImage = getIntent().getStringExtra("profileImage");

        // Set tampilan username dan foto
        usernameTextView.setText(profileUsername);
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.ic_favorit)
                    .error(R.drawable.ic_favorit)
                    .circleCrop()
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_favorit);
        }

        // Sembunyikan tombol jika melihat profil sendiri
        if (currentUserUid.equals(profileUsername)) {
            followButton.setVisibility(View.GONE);
            messageButton.setVisibility(View.GONE);
        } else {
            // Cek apakah sudah mengikuti
            checkIfFollowing();
            followButton.setOnClickListener(v -> {
                if (currentUserUid.equals(profileUsername)) {
                    Toast.makeText(UserProfileActivity.this, "Tidak bisa mengikuti diri sendiri", Toast.LENGTH_SHORT).show();
                } else {
                    isFollowing = !isFollowing;
                    updateFollowStatus();
                    updateFollowButton();
                    Toast.makeText(this, isFollowing ? "Mengikuti" : "Berhenti Mengikuti", Toast.LENGTH_SHORT).show();
                }
            });

            messageButton.setOnClickListener(v -> {
                // Cari UID berdasarkan username
                usersRef.orderByChild("username").equalTo(profileUsername)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot child : snapshot.getChildren()) {
                                        String profileUid = child.getKey(); // Ambil UID
                                        Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
                                        intent.putExtra("chatWithUid", profileUid); // Kirim UID ke ChatActivity
                                        startActivity(intent);
                                        break;
                                    }
                                } else {
                                    Toast.makeText(UserProfileActivity.this, "Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(UserProfileActivity.this, "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        updateFollowButton();
        loadFollowCounts();
        loadPostCount();
        fetchUserHistory();
    }

    private void updateFollowButton() {
        followButton.setText(isFollowing ? "Mengikuti" : "Ikuti");
    }

    private void checkIfFollowing() {
        followRef.child("following").child(currentUserUid)
                .child(profileUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        isFollowing = snap.exists();
                        updateFollowButton();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Log.e("FirebaseError", "checkIfFollowing: " + err.getMessage());
                    }
                });
    }

    private void updateFollowStatus() {
        if (isFollowing) {
            followRef.child("following").child(currentUserUid).child(profileUsername).setValue(true);
            followRef.child("followers").child(profileUsername).child(currentUserUid).setValue(true);
        } else {
            followRef.child("following").child(currentUserUid).child(profileUsername).removeValue();
            followRef.child("followers").child(profileUsername).child(currentUserUid).removeValue();
        }
        loadFollowCounts();  // Memanggil kembali untuk memperbarui count
    }

    private void loadFollowCounts() {
        // Hitung FOLLOWING (yang diikuti oleh profileUsername)
        followRef.child("following").child(profileUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        long count = snap.getChildrenCount();
                        followingCountTextView.setText(String.valueOf(count));  // Menampilkan di TextView
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Log.e("FirebaseError", "loadFollowingCount: " + err.getMessage());
                    }
                });

        // Hitung FOLLOWERS (yang mengikuti profileUsername)
        followRef.child("followers").child(profileUsername)  // Memperbaiki path referensi
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        long followers = snap.getChildrenCount();
                        followersCountTextView.setText(String.valueOf(followers));  // Menampilkan di TextView
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError err) {
                        Log.e("FirebaseError", "loadFollowersCount: " + err.getMessage());
                    }
                });
    }

    private void loadPostCount() {
        recipesRef.orderByChild("author")
                .equalTo(profileUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long postCount = snapshot.getChildrenCount();
                        postCountTextView.setText(String.valueOf(postCount));  // Menampilkan di TextView
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this,
                                "Gagal menghitung jumlah postingan: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", "loadPostCount: " + error.getMessage());
                    }
                });
    }

    private void fetchUserHistory() {
        recipesRef.orderByChild("author")
                .equalTo(profileUsername)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Recipe r = ds.getValue(Recipe.class);
                            if (r != null) historyList.add(r);
                        }
                        historyAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this,
                                "Gagal memuat riwayat: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseError", "fetchUserHistory: " + error.getMessage());
                    }
                });
    }
}

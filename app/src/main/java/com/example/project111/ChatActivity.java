package com.example.project111;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private EditText etMessage;
    private Button btnSend;

    private ImageView profileImageView;
    private TextView usernameTextView;

    private ChatAdapter adapter;
    private List<Message> messagesList;
    private List<String> keysList;

    private FirebaseAuth auth;
    private DatabaseReference messagesRef;
    private DatabaseReference userRef;

    private String currentUserId;
    private String otherUserId;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView       = findViewById(R.id.recyclerViewChat);
        etMessage          = findViewById(R.id.messageEditText);
        btnSend            = findViewById(R.id.sendMessageButton);
        profileImageView   = findViewById(R.id.profileImageView);
        usernameTextView   = findViewById(R.id.usernameTextView);

        auth           = FirebaseAuth.getInstance();
        currentUserId  = auth.getCurrentUser().getUid();
        otherUserId    = getIntent().getStringExtra("chatWithUid");

        // Validasi if the otherUserId is null
        if (otherUserId == null) {
            Toast.makeText(this, "Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get user profile data (username and profile image)
        userRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String profileImage = snapshot.child("profileImage").getValue(String.class);

                    usernameTextView.setText(username);
                    if (profileImage != null && !profileImage.isEmpty()) {
                        Glide.with(ChatActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_eye)
                                .circleCrop()
                                .into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "loadUserProfile error: " + error.getMessage());
            }
        });

        // Create unique and deterministic conversationId
        if (currentUserId.compareTo(otherUserId) < 0) {
            conversationId = currentUserId + "_" + otherUserId;
        } else {
            conversationId = otherUserId + "_" + currentUserId;
        }

        messagesRef = FirebaseDatabase.getInstance()
                .getReference("messages")
                .child(conversationId);

        messagesList = new ArrayList<>();
        keysList     = new ArrayList<>();
        adapter      = new ChatAdapter(messagesList, keysList, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Real-time listener to listen for new messages
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Message m = snapshot.getValue(Message.class);
                if (m != null) {
                    messagesList.add(m);
                    keysList.add(snapshot.getKey());
                    adapter.notifyItemInserted(messagesList.size() - 1);
                    recyclerView.scrollToPosition(messagesList.size() - 1);
                    Log.d("ChatActivity", "Pesan diterima: " + m.getText());
                } else {
                    Log.d("ChatActivity", "Pesan kosong diterima");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String key = snapshot.getKey();
                Message updated = snapshot.getValue(Message.class);
                if (key != null && updated != null) {
                    int idx = keysList.indexOf(key);
                    if (idx != -1) {
                        messagesList.set(idx, updated);
                        adapter.notifyItemChanged(idx);
                    }
                }
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                if (key != null) {
                    // Handle message removal from UI
                    int idx = keysList.indexOf(key);
                    if (idx != -1) {
                        messagesList.remove(idx);
                        keysList.remove(idx);
                        adapter.notifyItemRemoved(idx);
                    }
                }
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        Message message = new Message(currentUserId, otherUserId, text, timestamp);

        String key = messagesRef.push().getKey();
        if (key != null) {
            messagesRef.child(key).setValue(message)
                    .addOnSuccessListener(a -> {
                        etMessage.setText(""); // Clear the input field after sending
                        Log.d("ChatActivity", "Pesan berhasil dikirim: " + text);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        Log.e("ChatActivity", "Error sending message", e);
                    });
        } else {
            Log.e("ChatActivity", "Key Firebase gagal dibuat!");
        }
    }
}

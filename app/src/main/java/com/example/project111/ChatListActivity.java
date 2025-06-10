package com.example.project111;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ConversationAdapter conversationAdapter;
    private List<Message> messageList;
    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        recyclerView = findViewById(R.id.recyclerViewChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        messageList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(this, messageList);
        recyclerView.setAdapter(conversationAdapter);

        // Mendapatkan ID percakapan dari intent atau variabel lainnya
        String currentUserId = "ciwa5jqypzT1o9j5I5ibdvorsHL2"; // Ganti dengan ID pengguna yang valid
        String otherUserId = "xPdXiFBC8jZsPJB46hY8HWsz1Du2"; // Ganti dengan ID pengguna lain

        // Membuat conversationId yang unik
        String conversationId = currentUserId + "_" + otherUserId;

        // Mengambil data pesan dari Firebase berdasarkan conversationId
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(conversationId);

        // Mengambil data percakapan dari Firebase
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear(); // Membersihkan list sebelum menambah data baru
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                conversationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatListActivity.this, "Gagal memuat percakapan", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

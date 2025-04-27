package com.example.project111;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi komponen UI
        etEmail = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Cek status login
        checkLoginStatus();

        // Listener tombol login
        btnLogin.setOnClickListener(view -> loginUser());

        // Listener tombol register
        btnRegister.setOnClickListener(view -> {
            Intent registerIntent = new Intent(getApplicationContext(), register.class);
            startActivity(registerIntent);
        });
    }

    private void checkLoginStatus() {
        SharedPreferences preferences = getSharedPreferences("UserData", MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);
        String userId = preferences.getString("userId", null);

        if (isLoggedIn && userId != null) {
            // Sudah login, langsung pindah ke halaman perangkat (device)
            startActivity(new Intent(login.this, device.class));
            finish();
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email atau Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login menggunakan Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userId = user != null ? user.getUid() : "";

                        // Mengambil username dari Firebase Realtime Database
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                        userRef.child("username").get().addOnCompleteListener(usernameTask -> {
                            if (usernameTask.isSuccessful()) {
                                String username = usernameTask.getResult().getValue(String.class);

                                // Simpan status login dan userId ke SharedPreferences
                                SharedPreferences preferences = getSharedPreferences("UserData", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean("isLoggedIn", true); // Menyimpan status login
                                editor.putString("userId", userId);    // Menyimpan userId
                                editor.putString("username", username); // Menyimpan username
                                editor.apply();

                                Toast.makeText(getApplicationContext(), "Login Berhasil", Toast.LENGTH_SHORT).show();

                                // Pindah ke halaman device setelah login berhasil
                                Intent deviceIntent = new Intent(login.this, device.class);
                                startActivity(deviceIntent);
                                finish();
                            } else {
                                // Menampilkan pesan jika gagal mengambil username
                                Toast.makeText(getApplicationContext(), "Gagal mengambil data pengguna", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        // Menampilkan pesan jika login gagal
                        Toast.makeText(getApplicationContext(), "Login Gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

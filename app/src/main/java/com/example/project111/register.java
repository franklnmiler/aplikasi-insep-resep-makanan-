package com.example.project111;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etKonfirmasi;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inisialisasi elemen UI
        etUsername = findViewById(R.id.etusername);
        etEmail = findViewById(R.id.etemail);
        etPassword = findViewById(R.id.etpassword);
        etKonfirmasi = findViewById(R.id.etkonfirmasi);
        btnRegister = findViewById(R.id.btnregister);

        // Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String konfirmasi = etKonfirmasi.getText().toString().trim();

            // Validasi form
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(password) || TextUtils.isEmpty(konfirmasi)) {
                Toast.makeText(register.this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(konfirmasi)) {
                Toast.makeText(register.this, "Password dan konfirmasi tidak cocok!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Buat akun dengan Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            String userId = user != null ? user.getUid() : "";

                            // Siapkan data untuk disimpan di Firebase Realtime Database
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("email", email);
                            userData.put("password", password);
                            userData.put("userId", userId);
                            userData.put("device_info", android.os.Build.MODEL + " Brand: " + android.os.Build.BRAND + " Android Version: " + android.os.Build.VERSION.RELEASE);

                            database.child(userId).updateChildren(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Simpan ke SharedPreferences
                                        SharedPreferences preferences = getSharedPreferences("UserData", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putBoolean("isLoggedIn", false);  // Jangan langsung login
                                        editor.putString("userId", userId);
                                        editor.putString("username", username);
                                        editor.apply();

                                        Toast.makeText(register.this, "Registrasi berhasil. Silakan login.", Toast.LENGTH_SHORT).show();

                                        // Arahkan ke halaman login
                                        startActivity(new Intent(register.this, login.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(register.this, "Gagal menyimpan data ke database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            Toast.makeText(register.this, "Registrasi gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}

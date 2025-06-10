package com.example.project111;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private Button btnVerifikasi;
    private String verificationId;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    private String username, password, nomorHp;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        etOtp = findViewById(R.id.etotp);
        btnVerifikasi = findViewById(R.id.btnverifikasi);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        verificationId = getIntent().getStringExtra("verificationId");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        nomorHp = getIntent().getStringExtra("nomorHp");

        btnVerifikasi.setOnClickListener(view -> {
            String kode = etOtp.getText().toString().trim();
            if (kode.isEmpty()) {
                Toast.makeText(OtpActivity.this, "Masukkan Kode OTP", Toast.LENGTH_SHORT).show();
            } else {
                progressDialog = new ProgressDialog(OtpActivity.this);
                progressDialog.setMessage("Memverifikasi...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, kode);
                signInWithPhoneAuthCredential(credential);
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        DatabaseReference userRef = databaseRef.child(username);
                        userRef.child("userId").setValue(username);
                        userRef.child("username").setValue(username);
                        userRef.child("password").setValue(password);
                        userRef.child("nomorHp").setValue(nomorHp);

                        Toast.makeText(OtpActivity.this, "Registrasi & Verifikasi Berhasil", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(OtpActivity.this, login.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(OtpActivity.this, "Verifikasi Gagal", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

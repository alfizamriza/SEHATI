package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DaftarKantinActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private EditText editName, editNIS, editPassword, editConfirmPassword;
    private Button btnDaftar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daftar_kantin);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("Users/Kantin");

        // Initialize views
        editName = findViewById(R.id.editName);
        editNIS = findViewById(R.id.editNIS);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnDaftar = findViewById(R.id.btnDaftar);

        // Set click listener for register button
        btnDaftar.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String nis = editNIS.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String confirmPassword = editConfirmPassword.getText().toString().trim();

            // Validation
            if (name.isEmpty() || nis.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Password dan konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
                return;
            }

            btnDaftar.setEnabled(false);

            // Check if NIS is unique
            databaseReference.child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        btnDaftar.setEnabled(true);
                        Toast.makeText(DaftarKantinActivity.this, "NIS sudah terdaftar", Toast.LENGTH_SHORT).show();
                    } else {
                        // Register new kantin
                        Map<String, Object> kantinData = new HashMap<>();
                        kantinData.put("name", name);
                        kantinData.put("nis", nis);
                        kantinData.put("password", password);
                        kantinData.put("role", "kantin");
                        kantinData.put("saldo_kantin", 0);

                        databaseReference.child(nis).setValue(kantinData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("DaftarKantinActivity", "Kantin registered: " + nis);
                                    Toast.makeText(DaftarKantinActivity.this, "Pendaftaran berhasil, silakan login", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(DaftarKantinActivity.this, Login.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnDaftar.setEnabled(true);
                                    Log.e("DaftarKantinActivity", "Registration failed: " + e.getMessage());
                                    Toast.makeText(DaftarKantinActivity.this, "Pendaftaran gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    btnDaftar.setEnabled(true);
                    Log.e("DaftarKantinActivity", "Database error: " + error.getMessage());
                    Toast.makeText(DaftarKantinActivity.this, "Gagal memeriksa data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
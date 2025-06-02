package com.example.sehati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private Button btnMasuk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        btnMasuk = findViewById(R.id.btnMasuk);
//        Button btnDaftarGuru = findViewById(R.id.btnDaftarguru);
//        Button btnDaftarKantin = findViewById(R.id.btnDaftarkantin);
        EditText editNIS = findViewById(R.id.editNIS);
        EditText editSandi = findViewById(R.id.editSandi);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Set click listener for login button
        btnMasuk.setOnClickListener(v -> {
            String nis = editNIS.getText().toString().trim();
            String password = editSandi.getText().toString().trim();

            if (nis.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "NIS dan Password harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            btnMasuk.setEnabled(false);

            // Check if user is an admin
            databaseReference.child("Users").child("Admin").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        handleLogin(snapshot, nis, password, "admin", AdminActivity.class);
                    } else {
                        // Check if user is a teacher
                        databaseReference.child("Users").child("Teachers").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    handleLogin(snapshot, nis, password, "guru", Guru.class);
                                } else {
                                    // Check if user is a student
                                    databaseReference.child("Users").child("Students").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                handleLogin(snapshot, nis, password, "siswa", Murid.class);
                                            } else {
                                                // Check if user is a kantin
                                                databaseReference.child("Users").child("Kantin").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot snapshot) {
                                                        if (snapshot.exists()) {
                                                            handleLogin(snapshot, nis, password, "kantin", Kantin.class);
                                                        } else {
                                                            btnMasuk.setEnabled(true);
                                                            Toast.makeText(Login.this, "NIS tidak ditemukan", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError error) {
                                                        btnMasuk.setEnabled(true);
                                                        Toast.makeText(Login.this, "Gagal memeriksa data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            btnMasuk.setEnabled(true);
                                            Toast.makeText(Login.this, "Gagal memeriksa data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                btnMasuk.setEnabled(true);
                                Toast.makeText(Login.this, "Gagal memeriksa data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    btnMasuk.setEnabled(true);
                    Toast.makeText(Login.this, "Gagal memeriksa data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

//        // Set click listener for register teacher button
//        btnDaftarGuru.setOnClickListener(v -> {
//            Intent intent = new Intent(Login.this, DaftarGuruActivity.class);
//            startActivity(intent);
//        });
//
//        // Set click listener for register kantin button
//        btnDaftarKantin.setOnClickListener(v -> {
//            Intent intent = new Intent(Login.this, DaftarKantinActivity.class);
//            startActivity(intent);
//        });
    }

    private void handleLogin(DataSnapshot snapshot, String nis, String password, String expectedRole, Class<?> targetActivity) {
        String storedPassword = snapshot.child("password").getValue(String.class);
        String role = snapshot.child("role").getValue(String.class);

        if (storedPassword == null || role == null) {
            btnMasuk.setEnabled(true);
            Toast.makeText(Login.this, "Data pengguna tidak lengkap", Toast.LENGTH_SHORT).show();
            return;
        }

        if (storedPassword.equals(password) && role.equals(expectedRole)) {
            // Save login state
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("NIS", nis);
            editor.putString("ROLE", role);
            editor.apply();

            Intent intent = new Intent(Login.this, targetActivity);
            intent.putExtra("NIS", nis);
            startActivity(intent);
            finish();
        } else {
            btnMasuk.setEnabled(true);
            Toast.makeText(Login.this, role.equals(expectedRole) ? "Password salah" : "Role tidak valid", Toast.LENGTH_SHORT).show();
        }
    }
}
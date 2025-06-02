package com.example.sehati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private String adminNIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Get NIS from intent
        adminNIS = getIntent().getStringExtra("NIS");
        if (adminNIS == null) {
            Toast.makeText(this, "Sesi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnDaftarGuru = findViewById(R.id.btnDaftarGuru);
        Button btnDaftarKantin = findViewById(R.id.btnDaftarKantin);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Load admin name
        databaseReference.child("Users/Admin").child(adminNIS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                tvWelcome.setText("Halo, " + (name != null ? name : "Admin"));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set button listeners
        btnDaftarGuru.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, DaftarGuruActivity.class);
            startActivity(intent);
        });

        btnDaftarKantin.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, DaftarKantinActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // Clear login state
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // Navigate to Login
            Intent intent = new Intent(AdminActivity.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
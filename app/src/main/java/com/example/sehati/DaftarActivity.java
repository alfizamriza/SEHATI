package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DaftarActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private String teacherClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daftar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.daftar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find views
        EditText editTextName = findViewById(R.id.editTextName);
        EditText editTextNIS = findViewById(R.id.editTextNIS);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonAdd = findViewById(R.id.buttonAdd);
        ImageButton btnKembali = findViewById(R.id.btnKembali);

        // Get teacher NIS from Intent
        String teacherNIS = getIntent().getStringExtra("TEACHER_NIS");
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Fetch teacher's class
        if (teacherNIS != null) {
            buttonAdd.setEnabled(false); // Disable button until class is fetched
            databaseReference.child("Users").child("Teachers").child(teacherNIS).child("class")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            teacherClass = snapshot.getValue(String.class);
                            if (teacherClass != null) {
                                buttonAdd.setEnabled(true);
                            } else {
                                Toast.makeText(DaftarActivity.this, "Kelas guru tidak ditemukan", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(DaftarActivity.this, "Gagal mengambil kelas: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            Toast.makeText(this, "Data guru tidak valid", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Handle Add button click
        buttonAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String nis = editTextNIS.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (name.isEmpty() || nis.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show();
            } else if (teacherClass == null) {
                Toast.makeText(this, "Kelas guru belum diambil", Toast.LENGTH_SHORT).show();
            } else {
                // Save student data
                DatabaseReference studentRef = databaseReference.child("Users").child("Students").child(nis);
                studentRef.child("nis").setValue(nis);
                studentRef.child("name").setValue(name);
                studentRef.child("password").setValue(password);
                studentRef.child("role").setValue("siswa");
                studentRef.child("class").setValue(teacherClass);
                studentRef.child("koin").setValue(0);

                // Link student to class
                databaseReference.child("Classes").child(teacherClass).child("students").child(nis).setValue(true);

                Toast.makeText(this, "Siswa " + name + " ditambahkan", Toast.LENGTH_SHORT).show();

                // Return name to AbsenFragment
                Intent resultIntent = new Intent();
                resultIntent.putExtra("NAMA", name);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // Handle Back button click
        btnKembali.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}
package com.example.sehati;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DaftarGuruActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daftar_guru);

        EditText editTextName = findViewById(R.id.editTextName);
        EditText editTextNIS = findViewById(R.id.editTextNIS);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        EditText editTextClass = findViewById(R.id.editTextClass);
        Button buttonAdd = findViewById(R.id.buttonAdd);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child("Teachers");

        buttonAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String nis = editTextNIS.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String teacherClass = editTextClass.getText().toString().trim();

            if (name.isEmpty() || nis.isEmpty() || password.isEmpty() || teacherClass.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show();
            } else {
                DatabaseReference teacherRef = databaseReference.child(nis);
                teacherRef.child("name").setValue(name);
                teacherRef.child("password").setValue(password);
                teacherRef.child("role").setValue("guru");
                teacherRef.child("class").setValue(teacherClass);

                // Initialize class in Classes node
                DatabaseReference classRef = FirebaseDatabase.getInstance().getReference("Classes").child(teacherClass);
                classRef.child("teacher").setValue(nis);

                Toast.makeText(this, "Guru " + name + " ditambahkan", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
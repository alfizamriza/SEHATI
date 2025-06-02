package com.example.sehati;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PelaporanFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String teacherNIS;
    private Spinner classSpinner;
    private Spinner studentSpinner;
    private RadioGroup violationRadioGroup;
    private Button sendButton;
    private LinearLayout historyContainer;
    private List<String> classList;
    private Map<String, List<Student>> classStudentsMap;

    private static class Student {
        String nis;
        String name;

        Student(String nis, String name) {
            this.nis = nis;
            this.name = name;
        }
    }

    public PelaporanFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (getArguments() != null) {
            teacherNIS = getArguments().getString("NIS");
        }
        classList = new ArrayList<>();
        classStudentsMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d("PelaporanFragment", "onCreateView started with teacherNIS: " + teacherNIS);
            View view = inflater.inflate(R.layout.fragment_pelaporan, container, false);

            // Initialize views
            classSpinner = view.findViewById(R.id.spinner_kelas);
            studentSpinner = view.findViewById(R.id.spinner_kelas2);
            violationRadioGroup = view.findViewById(R.id.violation_radio_group);
            sendButton = view.findViewById(R.id.send_button);
            historyContainer = view.findViewById(R.id.history_container);

            // Check if views are found
            if (classSpinner == null || studentSpinner == null || violationRadioGroup == null ||
                    sendButton == null || historyContainer == null) {
                Toast.makeText(getContext(), "Gagal memuat tampilan", Toast.LENGTH_LONG).show();
                return view;
            }

            // Set up send button
            sendButton.setOnClickListener(v -> submitReport());

            // Load classes and history
            loadClasses();
            loadViolationHistory();

            return view;
        } catch (Exception e) {
            Log.e("PelaporanFragment", "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error memuat pelaporan: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    private void loadClasses() {
        databaseReference.child("Classes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                classList.clear();
                classStudentsMap.clear();

                for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                    String className = classSnapshot.getKey();
                    classList.add(className);

                    List<Student> students = new ArrayList<>();
                    DataSnapshot studentsSnapshot = classSnapshot.child("students");
                    for (DataSnapshot studentSnapshot : studentsSnapshot.getChildren()) {
                        String studentNIS = studentSnapshot.getKey();
                        databaseReference.child("Users").child("Students").child(studentNIS).child("name")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                        String studentName = nameSnapshot.getValue(String.class);
                                        if (studentName != null) {
                                            students.add(new Student(studentNIS, studentName));
                                            // Update student spinner if this class is selected
                                            if (classSpinner.getSelectedItem() != null &&
                                                    classSpinner.getSelectedItem().toString().equals(className)) {
                                                updateStudentSpinner(className);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("PelaporanFragment", "Failed to load student name: " + error.getMessage());
                                    }
                                });
                    }
                    classStudentsMap.put(className, students);
                }

                // Update class spinner
                ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                        getContext(), android.R.layout.simple_spinner_item, classList);
                classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                classSpinner.setAdapter(classAdapter);

                // Set up class spinner listener
                classSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        String selectedClass = classList.get(position);
                        updateStudentSpinner(selectedClass);
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        studentSpinner.setAdapter(null);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PelaporanFragment", "Failed to load classes: " + error.getMessage());
                Toast.makeText(getContext(), "Gagal memuat kelas: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStudentSpinner(String className) {
        List<Student> students = classStudentsMap.getOrDefault(className, new ArrayList<>());
        List<String> studentNames = new ArrayList<>();
        for (Student student : students) {
            studentNames.add(student.name);
        }

        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, studentNames);
        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        studentSpinner.setAdapter(studentAdapter);
    }

    private void submitReport() {
        if (teacherNIS == null) {
            Toast.makeText(getContext(), "Data guru tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs
        if (classSpinner.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Pilih kelas", Toast.LENGTH_SHORT).show();
            return;
        }
        if (studentSpinner.getSelectedItem() == null) {
            Toast.makeText(getContext(), "Pilih siswa", Toast.LENGTH_SHORT).show();
            return;
        }
        int checkedRadioButtonId = violationRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == -1) {
            Toast.makeText(getContext(), "Pilih jenis pelanggaran", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get violation type safely
        RadioButton selectedRadioButton = violationRadioGroup.findViewById(checkedRadioButtonId);
        if (selectedRadioButton == null) {
            Log.e("PelaporanFragment", "Selected RadioButton is null for ID: " + checkedRadioButtonId);
            Toast.makeText(getContext(), "Gagal memilih pelanggaran", Toast.LENGTH_SHORT).show();
            return;
        }
        String violationType = selectedRadioButton.getText().toString();
        Log.d("PelaporanFragment", "Selected violation type: " + violationType);

        String selectedClass = classSpinner.getSelectedItem().toString();
        String selectedStudentName = studentSpinner.getSelectedItem().toString();

        // Find student NIS
        List<Student> students = classStudentsMap.get(selectedClass);
        String studentNIS = null;
        for (Student student : students) {
            if (student.name.equals(selectedStudentName)) {
                studentNIS = student.nis;
                break;
            }
        }

        if (studentNIS == null) {
            Toast.makeText(getContext(), "Siswa tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare report data
        String reportId = databaseReference.child("Pelanggaran").push().getKey();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long timestamp = System.currentTimeMillis();

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("teacher_nis", teacherNIS);
        reportData.put("student_nis", studentNIS);
        reportData.put("class", selectedClass);
        reportData.put("violation_type", violationType);
        reportData.put("date", date);
        reportData.put("timestamp", timestamp);

        // Save report
        Map<String, Object> updates = new HashMap<>();
        updates.put("Pelanggaran/" + reportId, reportData);
        updates.put("Users/Students/" + studentNIS + "/violations/" + reportId, reportData);

        databaseReference.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Laporan berhasil dikirim", Toast.LENGTH_SHORT).show();
                    loadViolationHistory(); // Refresh history
                    violationRadioGroup.clearCheck();
                    studentSpinner.setSelection(0);
                })
                .addOnFailureListener(e -> {
                    Log.e("PelaporanFragment", "Failed to save report: " + e.getMessage());
                    Toast.makeText(getContext(), "Gagal mengirim laporan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void loadViolationHistory() {
        if (teacherNIS == null) {
            return;
        }

        databaseReference.child("Pelanggaran")
                .orderByChild("teacher_nis").equalTo(teacherNIS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyContainer.removeAllViews();

                        for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                            String studentNIS = reportSnapshot.child("student_nis").getValue(String.class);
                            String className = reportSnapshot.child("class").getValue(String.class);
                            String violationType = reportSnapshot.child("violation_type").getValue(String.class);
                            String date = reportSnapshot.child("date").getValue(String.class);

                            if (studentNIS == null || className == null || violationType == null || date == null) {
                                continue;
                            }

                            // Get student name
                            databaseReference.child("Users").child("Students").child(studentNIS).child("name")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                            String studentName = nameSnapshot.getValue(String.class);
                                            if (studentName != null) {
                                                addHistoryRow(studentName, className, violationType, date);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("PelaporanFragment", "Failed to load student name for history: " + error.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PelaporanFragment", "Failed to load violation history: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat riwayat: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addHistoryRow(String studentName, String className, String violationType, String date) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(8, 8, 8, 8);
        row.setBackgroundResource(R.drawable.box_white);

        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setText(String.format("Siswa: %s\nKelas: %s\nPelanggaran: %s\nTanggal: %s",
                studentName, className, violationType, date));
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setPadding(8, 8, 8, 8);

        row.addView(textView);
        historyContainer.addView(row);
    }
}
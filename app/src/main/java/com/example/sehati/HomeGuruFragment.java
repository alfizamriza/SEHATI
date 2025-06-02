package com.example.sehati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeGuruFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String teacherNIS;
    private String teacherClass;
    private TextView welcomeText;
    private TextView selamatDatang;
    private TextView totalStudentsText;
    private TextView tumblerText;
    private TextView wadahText;
    private TextView togebagText;
    private ProgressBar progressBar;
    private Button logoutButton;

    public HomeGuruFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (getArguments() != null) {
            teacherNIS = getArguments().getString("NIS");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d("HomeGuruFragment", "onCreateView started with teacherNIS: " + teacherNIS);
            View view = inflater.inflate(R.layout.fragment_home_guru, container, false);

            // Initialize views
            welcomeText = view.findViewById(R.id.welcome_text);
            selamatDatang = view.findViewById(R.id.selamat_datang);
            totalStudentsText = view.findViewById(R.id.total_students_text);
            tumblerText = view.findViewById(R.id.tumbler_text);
            wadahText = view.findViewById(R.id.wadah_text);
            togebagText = view.findViewById(R.id.togebag_text);
            progressBar = view.findViewById(R.id.progress_bar);
            logoutButton = view.findViewById(R.id.logout_button);

            // Check if views are found
            if (welcomeText == null || selamatDatang == null || totalStudentsText == null ||
                    tumblerText == null || wadahText == null || togebagText == null ||
                    progressBar == null || logoutButton == null) {
                Toast.makeText(getContext(), "Gagal memuat tampilan", Toast.LENGTH_LONG).show();
                return view;
            }

            // Set up logout button
            logoutButton.setOnClickListener(v -> logout());

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);

            // Load teacher data and statistics
            loadTeacherData();

            return view;
        } catch (Exception e) {
            Log.e("HomeGuruFragment", "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error memuat halaman: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    private void loadTeacherData() {
        if (teacherNIS == null) {
            Toast.makeText(getContext(), "Data guru tidak valid", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Load teacher name and class
        databaseReference.child("Users").child("Teachers").child(teacherNIS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String teacherName = snapshot.child("name").getValue(String.class);
                        teacherClass = snapshot.child("class").getValue(String.class);

                        if (teacherName != null) {
                            welcomeText.setText("Halo, " + teacherNIS);
                            selamatDatang.setText(teacherName);
                        } else {
                            welcomeText.setText("Halo, " + teacherNIS);
                            selamatDatang.setText("Guru");
                        }

                        if (teacherClass != null) {
                            loadStatistics();
                        } else {
                            Toast.makeText(getContext(), "Kelas guru tidak ditemukan", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeGuruFragment", "Failed to load teacher data: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat data guru: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadStatistics() {
        // Load total students
        databaseReference.child("Classes").child(teacherClass).child("students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalStudents = snapshot.getChildrenCount();
                        totalStudentsText.setText(totalStudents + " Siswa");
                        loadWeeklyAttendance();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeGuruFragment", "Failed to load students: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat data siswa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadWeeklyAttendance() {
        // Calculate dates for the last 7 days
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Map<String, Integer> tumblerCount = new HashMap<>();
        Map<String, Integer> wadahCount = new HashMap<>();
        Map<String, Integer> togebagCount = new HashMap<>();

        // Initialize counts for each date
        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());
            tumblerCount.put(date, 0);
            wadahCount.put(date, 0);
            togebagCount.put(date, 0);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Load attendance data for the last 7 days
        DatabaseReference absensiRef = databaseReference.child("Absensi");
        calendar = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            String date = dateFormat.format(calendar.getTime());
            // Skip Sundays
            if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                absensiRef.child(date).child(teacherClass).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int tumbler = 0, wadah = 0, togebag = 0;
                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                            Boolean tumblerValue = studentSnapshot.child("tumbler").getValue(Boolean.class);
                            Boolean wadahValue = studentSnapshot.child("wadah").getValue(Boolean.class);
                            Boolean togebagValue = studentSnapshot.child("togebag").getValue(Boolean.class);
                            if (tumblerValue != null && tumblerValue) tumbler++;
                            if (wadahValue != null && wadahValue) wadah++;
                            if (togebagValue != null && togebagValue) togebag++;
                        }
                        tumblerCount.put(date, tumbler);
                        wadahCount.put(date, wadah);
                        togebagCount.put(date, togebag);
                        updateAttendanceUI(tumblerCount, wadahCount, togebagCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeGuruFragment", "Failed to load attendance for " + date + ": " + error.getMessage());
                        updateAttendanceUI(tumblerCount, wadahCount, togebagCount);
                    }
                });
            } else {
                updateAttendanceUI(tumblerCount, wadahCount, togebagCount);
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void updateAttendanceUI(Map<String, Integer> tumblerCount, Map<String, Integer> wadahCount, Map<String, Integer> togebagCount) {
        int totalTumbler = 0, totalWadah = 0, totalTogebag = 0;
        for (int count : tumblerCount.values()) totalTumbler += count;
        for (int count : wadahCount.values()) totalWadah += count;
        for (int count : togebagCount.values()) totalTogebag += count;

        tumblerText.setText("Tumbler: " + totalTumbler);
        wadahText.setText("Bekal/Wadah: " + totalWadah);
        togebagText.setText("Togebag: " + totalTogebag);
        progressBar.setVisibility(View.GONE);
    }

    private void logout() {
        // Clear login status
        SharedPreferences prefs = getActivity().getSharedPreferences("LoginPrefs", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Navigate to MainActivity
        Intent intent = new Intent(getActivity(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
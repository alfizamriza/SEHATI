package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AbsenFragment extends Fragment {

    private static final int REQUEST_CODE_ADD_STUDENT = 100;
    private static final String KEY_ATTENDANCE_STATE = "attendance_state";
    private LinearLayout studentListContainer;
    private String teacherNIS;
    private String teacherClass;
    private DatabaseReference databaseReference;
    private Button btnKirimAbsen;
    private Map<String, RadioGroup> tumblerGroups = new HashMap<>();
    private Map<String, RadioGroup> wadahGroups = new HashMap<>();
    private Map<String, RadioGroup> togebagGroups = new HashMap<>();
    private Map<String, Map<String, Boolean>> savedAttendanceState = new HashMap<>();

    public AbsenFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teacherNIS = getArguments().getString("NIS");
        }
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d("AbsenFragment", "onCreateView started with teacherNIS: " + teacherNIS);
            View view = inflater.inflate(R.layout.fragment_absen, container, false);
            studentListContainer = view.findViewById(R.id.student_list_container);
            btnKirimAbsen = view.findViewById(R.id.btn_kirim_absen);
            Button btnTambahSiswa = view.findViewById(R.id.btn_tambah_siswa);

            if (studentListContainer == null || btnKirimAbsen == null || btnTambahSiswa == null) {
                Toast.makeText(getContext(), "Gagal memuat tampilan absensi", Toast.LENGTH_LONG).show();
                return view;
            }

            btnTambahSiswa.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), DaftarActivity.class);
                intent.putExtra("TEACHER_NIS", teacherNIS);
                startActivityForResult(intent, REQUEST_CODE_ADD_STUDENT);
            });

            btnKirimAbsen.setOnClickListener(v -> submitAttendance());

            if (savedInstanceState != null) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Boolean>> restoredState = (Map<String, Map<String, Boolean>>) savedInstanceState.getSerializable(KEY_ATTENDANCE_STATE);
                if (restoredState != null) {
                    savedAttendanceState = restoredState;
                }
            }

            checkAttendanceAvailability();

            return view;
        } catch (Exception e) {
            Log.e("AbsenFragment", "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error memuat absensi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Map<String, Map<String, Boolean>> currentState = new HashMap<>();
        for (String studentNIS : tumblerGroups.keySet()) {
            Map<String, Boolean> studentState = new HashMap<>();
            studentState.put("tumbler", isRadioYesSelected(tumblerGroups.get(studentNIS), "tumbler_" + studentNIS + "_yes"));
            studentState.put("wadah", isRadioYesSelected(wadahGroups.get(studentNIS), "wadah_" + studentNIS + "_yes"));
            studentState.put("togebag", isRadioYesSelected(togebagGroups.get(studentNIS), "togebag_" + studentNIS + "_yes"));
            currentState.put(studentNIS, studentState);
        }
        outState.putSerializable(KEY_ATTENDANCE_STATE, (java.io.Serializable) currentState);
    }

    private void checkAttendanceAvailability() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            btnKirimAbsen.setEnabled(false);
            Toast.makeText(getContext(), "Absensi tidak tersedia di hari Minggu", Toast.LENGTH_LONG).show();
            loadStudents();
            return;
        }

        loadTeacherClass(() -> loadStudents());
    }

    private void loadTeacherClass(Runnable callback) {
        if (teacherNIS == null) {
            Toast.makeText(getContext(), "Data guru tidak valid", Toast.LENGTH_SHORT).show();
            callback.run();
            return;
        }

        databaseReference.child("Users").child("Teachers").child(teacherNIS).child("class")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        teacherClass = snapshot.getValue(String.class);
                        if (teacherClass != null) {
                            TextView classTextView = getView() != null ? getView().findViewById(R.id.class_text_view) : null;
                            if (classTextView != null) {
                                classTextView.setText(teacherClass);
                            }
                        }
                        callback.run();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("AbsenFragment", "Failed to load teacher class: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat kelas: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        callback.run();
                    }
                });
    }

    private void loadStudents() {
        if (teacherClass == null) {
            Toast.makeText(getContext(), "Kelas guru tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        databaseReference.child("Classes").child(teacherClass).child("students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        studentListContainer.removeAllViews();
                        tumblerGroups.clear();
                        wadahGroups.clear();
                        togebagGroups.clear();

                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            Toast.makeText(getContext(), "Tidak ada siswa di kelas ini", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                            String studentNIS = studentSnapshot.getKey();
                            databaseReference.child("Users").child("Students").child(studentNIS).child("name")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot nameSnapshot) {
                                            String studentName = nameSnapshot.getValue(String.class);
                                            if (studentName != null) {
                                                addStudentRow(studentListContainer, studentName, studentNIS, todayDate);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            Log.e("AbsenFragment", "Failed to load student name: " + error.getMessage());
                                            Toast.makeText(getContext(), "Gagal memuat nama siswa", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("AbsenFragment", "Failed to load students: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat siswa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addStudentRow(LinearLayout container, String studentName, String studentNIS, String todayDate) {
        LinearLayout studentRow = new LinearLayout(getContext());
        studentRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        studentRow.setOrientation(LinearLayout.HORIZONTAL);
        studentRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        studentRow.setPadding(8, 8, 8, 8);

        TextView nameTextView = new TextView(getContext());
        nameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        nameTextView.setText(studentName);
        nameTextView.setTextSize(16);

        RadioGroup radioGroupTumbler = createRadioGroup("tumbler_" + studentNIS);
        RadioGroup radioGroupWadah = createRadioGroup("wadah_" + studentNIS);
        RadioGroup radioGroupTogebag = createRadioGroup("togebag_" + studentNIS);

        tumblerGroups.put(studentNIS, radioGroupTumbler);
        wadahGroups.put(studentNIS, radioGroupWadah);
        togebagGroups.put(studentNIS, radioGroupTogebag);

        radioGroupTumbler.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadio = group.findViewById(checkedId);
            String status = checkedRadio != null && checkedRadio.getTag().toString().endsWith("_yes") ? "Ya" : "Tidak";
            Log.d("AbsenFragment", studentName + " Tumbler: " + status);
        });

        radioGroupWadah.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadio = group.findViewById(checkedId);
            String status = checkedRadio != null && checkedRadio.getTag().toString().endsWith("_yes") ? "Ya" : "Tidak";
            Log.d("AbsenFragment", studentName + " Wadah: " + status);
        });

        radioGroupTogebag.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadio = group.findViewById(checkedId);
            String status = checkedRadio != null && checkedRadio.getTag().toString().endsWith("_yes") ? "Ya" : "Tidak";
            Log.d("AbsenFragment", studentName + " Togebag: " + status);
        });

        studentRow.addView(nameTextView);
        studentRow.addView(radioGroupTumbler);
        studentRow.addView(radioGroupWadah);
        studentRow.addView(radioGroupTogebag);
        container.addView(studentRow);

        if (savedAttendanceState.containsKey(studentNIS)) {
            Map<String, Boolean> studentState = savedAttendanceState.get(studentNIS);
            setRadioGroupState(radioGroupTumbler, studentState.get("tumbler"), "tumbler_" + studentNIS);
            setRadioGroupState(radioGroupWadah, studentState.get("wadah"), "wadah_" + studentNIS);
            setRadioGroupState(radioGroupTogebag, studentState.get("togebag"), "togebag_" + studentNIS);
        } else {
            databaseReference.child("Absensi").child(todayDate).child(teacherClass).child(studentNIS)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Boolean tumbler = snapshot.child("tumbler").getValue(Boolean.class);
                                Boolean wadah = snapshot.child("wadah").getValue(Boolean.class);
                                Boolean togebag = snapshot.child("togebag").getValue(Boolean.class);
                                setRadioGroupState(radioGroupTumbler, tumbler, "tumbler_" + studentNIS);
                                setRadioGroupState(radioGroupWadah, wadah, "wadah_" + studentNIS);
                                setRadioGroupState(radioGroupTogebag, togebag, "togebag_" + studentNIS);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e("AbsenFragment", "Failed to load previous attendance: " + error.getMessage());
                        }
                    });
        }
    }

    private void setRadioGroupState(RadioGroup radioGroup, Boolean value, String prefix) {
        if (value != null) {
            int radioId = value ? radioGroup.findViewWithTag(prefix + "_yes").getId() : radioGroup.findViewWithTag(prefix + "_no").getId();
            radioGroup.check(radioId);
        }
    }

    private RadioGroup createRadioGroup(String idPrefix) {
        RadioGroup radioGroup = new RadioGroup(getContext());
        radioGroup.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        radioGroup.setOrientation(LinearLayout.HORIZONTAL);
        radioGroup.setGravity(android.view.Gravity.CENTER);

        RadioButton radioYes = new RadioButton(getContext());
        radioYes.setId(View.generateViewId());
        radioYes.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        radioYes.setButtonDrawable(R.drawable.radio_buttom_yes);
        radioYes.setPadding(4, 0, 4, 0);
        radioYes.setTag(idPrefix + "_yes");

        RadioButton radioNo = new RadioButton(getContext());
        radioNo.setId(View.generateViewId());
        radioNo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        radioNo.setButtonDrawable(R.drawable.radio_buttom_no);
        radioNo.setPadding(4, 0, 4, 0);
        radioNo.setTag(idPrefix + "_no");

        radioGroup.addView(radioYes);
        radioGroup.addView(radioNo);
        return radioGroup;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_STUDENT && resultCode == getActivity().RESULT_OK && data != null) {
            String studentName = data.getStringExtra("NAMA");
            if (studentName != null && !studentName.isEmpty()) {
                loadStudents();
                Toast.makeText(getContext(), "Siswa " + studentName + " ditambahkan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitAttendance() {
        if (teacherClass == null) {
            Toast.makeText(getContext(), "Kelas guru tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String studentNIS : tumblerGroups.keySet()) {
            RadioGroup tumblerGroup = tumblerGroups.get(studentNIS);
            RadioGroup wadahGroup = wadahGroups.get(studentNIS);
            RadioGroup togebagGroup = togebagGroups.get(studentNIS);
            if (tumblerGroup.getCheckedRadioButtonId() == -1 || wadahGroup.getCheckedRadioButtonId() == -1 || togebagGroup.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getContext(), "Pilih status untuk semua item siswa", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String weekId = new SimpleDateFormat("yyyy-WW", Locale.US).format(new Date());
        DatabaseReference absensiRef = databaseReference.child("Absensi").child(todayDate).child(teacherClass);

        Map<String, Map<String, Object>> attendanceData = new HashMap<>();
        for (String studentNIS : tumblerGroups.keySet()) {
            RadioGroup tumblerGroup = tumblerGroups.get(studentNIS);
            RadioGroup wadahGroup = wadahGroups.get(studentNIS);
            RadioGroup togebagGroup = togebagGroups.get(studentNIS);

            boolean tumbler = isRadioYesSelected(tumblerGroup, "tumbler_" + studentNIS + "_yes");
            boolean wadah = isRadioYesSelected(wadahGroup, "wadah_" + studentNIS + "_yes");
            boolean togebag = isRadioYesSelected(togebagGroup, "togebag_" + studentNIS + "_yes");

            int koinChange = (tumbler ? 5 : -5) + (wadah ? 5 : -5) + (togebag ? 5 : -5);

            Map<String, Object> studentAttendance = new HashMap<>();
            studentAttendance.put("tumbler", tumbler);
            studentAttendance.put("wadah", wadah);
            studentAttendance.put("togebag", togebag);
            studentAttendance.put("koin_change", koinChange);
            attendanceData.put(studentNIS, studentAttendance);

            // Update student's koin and ClassRankings
            DatabaseReference studentKoinRef = databaseReference.child("Users").child("Students").child(studentNIS).child("koin");
            DatabaseReference classKoinRef = databaseReference.child("ClassRankings").child(weekId).child("classes").child(teacherClass);
            studentKoinRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Long currentKoin = snapshot.getValue(Long.class);
                    if (currentKoin == null) currentKoin = 0L;
                    long newKoin = Math.max(0, currentKoin + koinChange);
                    studentKoinRef.setValue(newKoin);

                    // Update ClassRankings
                    classKoinRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot classSnapshot) {
                            Long currentClassKoin = classSnapshot.child("total_koin").getValue(Long.class);
                            if (currentClassKoin == null) currentClassKoin = 0L;
                            long newClassKoin = Math.max(0, currentClassKoin + koinChange);
                            Map<String, Object> classUpdate = new HashMap<>();
                            classUpdate.put("total_koin", newClassKoin);
                            classUpdate.put("last_updated", ServerValue.TIMESTAMP);
                            classKoinRef.setValue(classUpdate);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e("AbsenFragment", "Failed to update class koin: " + error.getMessage());
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("AbsenFragment", "Failed to update koin: " + error.getMessage());
                }
            });
        }

        absensiRef.setValue(attendanceData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Absensi berhasil diperbarui", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("AbsenFragment", "Failed to save attendance: " + e.getMessage());
                    Toast.makeText(getContext(), "Gagal menyimpan absensi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isRadioYesSelected(RadioGroup radioGroup, String yesTag) {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) return false;
        RadioButton checked = radioGroup.findViewById(checkedId);
        return checked != null && checked.getTag().toString().equals(yesTag);
    }
}
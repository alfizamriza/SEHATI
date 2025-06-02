package com.example.sehati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeMuridFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String studentNIS;
    private TextView tvWelcome, tvKoinAmount, tvCardNumber, tvEmptyViolations, tvEmptyExchange;
    private LinearLayout violationHistoryContainer, exchangeHistoryContainer;
    private ProgressBar progressBar;
    private ImageButton logoutButton;

    public HomeMuridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (getArguments() != null) {
            studentNIS = getArguments().getString("NIS");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            Log.d("HomeMuridFragment", "onCreateView started with studentNIS: " + studentNIS);
            View view = inflater.inflate(R.layout.fragment_home_murid, container, false);

            // Initialize views
            tvWelcome = view.findViewById(R.id.tvWelcome);
            tvKoinAmount = view.findViewById(R.id.tvKoinAmount);
            tvCardNumber = view.findViewById(R.id.tvCardNumber);
            violationHistoryContainer = view.findViewById(R.id.violation_history_container);
            exchangeHistoryContainer = view.findViewById(R.id.exchange_history_container);
            tvEmptyViolations = view.findViewById(R.id.tvEmptyViolations);
            tvEmptyExchange = view.findViewById(R.id.tvEmptyExchange);
            progressBar = view.findViewById(R.id.progressBar);
            logoutButton = view.findViewById(R.id.logout_button);

            // Check if views are found
            if (tvWelcome == null || tvKoinAmount == null || tvCardNumber == null ||
                    violationHistoryContainer == null || exchangeHistoryContainer == null ||
                    tvEmptyViolations == null || tvEmptyExchange == null ||
                    progressBar == null || logoutButton == null) {
                Toast.makeText(getContext(), "Gagal memuat tampilan", Toast.LENGTH_LONG).show();
                return view;
            }

            // Set up logout button
            logoutButton.setOnClickListener(v -> logout());

            // Show progress bar
            progressBar.setVisibility(View.VISIBLE);

            // Load student data
            loadStudentData();

            return view;
        } catch (Exception e) {
            Log.e("HomeMuridFragment", "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Terjadi kesalahan saat memuat halaman", Toast.LENGTH_LONG).show();
            return new View(getContext());
        }
    }

    private void loadStudentData() {
        if (studentNIS == null || getContext() == null) {
            Toast.makeText(getContext(), "Data siswa tidak valid", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Load student name, koin, and NIS
        databaseReference.child("Users/Students").child(studentNIS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String studentName = snapshot.child("name").getValue(String.class);
                        Long koin = snapshot.child("koin").getValue(Long.class);

                        if (studentName != null) {
                            tvWelcome.setText("Halo, " + studentName);
                        } else {
                            tvWelcome.setText("Halo, Siswa");
                        }

                        if (koin != null) {
                            tvKoinAmount.setText(String.format("%,d", koin));
                        } else {
                            tvKoinAmount.setText("0");
                        }

                        tvCardNumber.setText("NIS: " + studentNIS);

                        loadViolationHistory();
                        loadExchangeHistory();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeMuridFragment", "Failed to load student data: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat data siswa", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadViolationHistory() {
        databaseReference.child("Users/Students").child(studentNIS).child("violations")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        violationHistoryContainer.removeAllViews();
                        boolean hasViolations = false;

                        for (DataSnapshot violationSnapshot : snapshot.getChildren()) {
                            String teacherNIS = violationSnapshot.child("teacher_nis").getValue(String.class);
                            String violationType = violationSnapshot.child("violation_type").getValue(String.class);
                            String date = violationSnapshot.child("date").getValue(String.class);

                            if (teacherNIS == null || violationType == null || date == null) {
                                continue;
                            }

                            hasViolations = true;

                            // Get teacher name
                            databaseReference.child("Users/Teachers").child(teacherNIS).child("name")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                                            String teacherName = nameSnapshot.getValue(String.class);
                                            if (teacherName == null) {
                                                teacherName = "Guru";
                                            }
                                            addViolationRow(violationType, date, teacherName);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("HomeMuridFragment", "Failed to load teacher name: " + error.getMessage());
                                            addViolationRow(violationType, date, "Guru");
                                        }
                                    });
                        }

                        tvEmptyViolations.setVisibility(hasViolations ? View.GONE : View.VISIBLE);
                        if (!snapshot.hasChildren()) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeMuridFragment", "Failed to load violations: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat riwayat pelanggaran", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void loadExchangeHistory() {
        databaseReference.child("Transactions")
                .orderByChild("user_nis").equalTo(studentNIS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        exchangeHistoryContainer.removeAllViews();
                        boolean hasExchanges = false;

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                        for (DataSnapshot txSnapshot : snapshot.getChildren()) {
                            String type = txSnapshot.child("type").getValue(String.class);
                            Long amount = txSnapshot.child("amount").getValue(Long.class);
                            Long timestamp = txSnapshot.child("timestamp").getValue(Long.class);
                            String targetName = txSnapshot.child("target_name").getValue(String.class);

                            if (type == null || amount == null || timestamp == null) {
                                continue;
                            }

                            hasExchanges = true;

                            String displayText;
                            String detailText = "";
                            switch (type) {
                                case "koin_to_rupiah":
                                    displayText = "Penukaran Rupiah";
                                    detailText = "Rp " + String.format("%,d", amount);
                                    break;
                                case "koin_to_item":
                                    displayText = "Penukaran Item";
                                    detailText = amount + " koin";
                                    break;
                                case "kupon_exchange":
                                    displayText = "Penukaran Kupon";
                                    detailText = amount + " kupon ke " + (targetName != null ? targetName : "Kantin");
                                    break;
                                default:
                                    displayText = "Transaksi";
                                    detailText = amount + " koin";
                            }

                            String dateStr = sdf.format(new Date(timestamp));
                            addExchangeRow(displayText, detailText, dateStr);
                        }

                        tvEmptyExchange.setVisibility(hasExchanges ? View.GONE : View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeMuridFragment", "Failed to load exchange history: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat riwayat penukaran", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void addViolationRow(String violationType, String date, String teacherName) {
        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(4);
        cardView.setRadius(12);

        LinearLayout row = new LinearLayout(requireContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundResource(android.R.color.white);

        TextView typeTextView = new TextView(requireContext());
        typeTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        typeTextView.setText("Pelanggaran: " + violationType);
        typeTextView.setTextSize(14);
        typeTextView.setTextColor(getResources().getColor(android.R.color.black));
        typeTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView teacherTextView = new TextView(requireContext());
        teacherTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        teacherTextView.setText("Dilaporkan oleh: " + teacherName);
        teacherTextView.setTextSize(14);
        teacherTextView.setTextColor(getResources().getColor(android.R.color.black));

        TextView dateTextView = new TextView(requireContext());
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        dateTextView.setText(date);
        dateTextView.setTextSize(12);
        dateTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));

        row.addView(typeTextView);
        row.addView(teacherTextView);
        row.addView(dateTextView);
        cardView.addView(row);
        violationHistoryContainer.addView(cardView);
    }

    private void addExchangeRow(String title, String detail, String date) {
        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 8, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(4);
        cardView.setRadius(12);

        LinearLayout row = new LinearLayout(requireContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundResource(android.R.color.white);

        TextView titleTextView = new TextView(requireContext());
        titleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        titleTextView.setText(title);
        titleTextView.setTextSize(14);
        titleTextView.setTextColor(getResources().getColor(android.R.color.black));
        titleTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView detailTextView = new TextView(requireContext());
        detailTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        detailTextView.setText(detail);
        detailTextView.setTextSize(14);
        detailTextView.setTextColor(getResources().getColor(android.R.color.black));

        TextView dateTextView = new TextView(requireContext());
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        dateTextView.setText(date);
        dateTextView.setTextSize(12);
        dateTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));

        row.addView(titleTextView);
        row.addView(detailTextView);
        row.addView(dateTextView);
        cardView.addView(row);
        exchangeHistoryContainer.addView(cardView);
    }

    private void logout() {
        if (getContext() == null) return;
        // Clear login status
        SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Navigate to MainActivity
        Intent intent = new Intent(getActivity(), HomeKantinFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeKantinFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String nis;
    private TextView headerText, totalAmount;
    private ImageButton keluarButton;
    private LinearLayout historyLayout;
    private View exchangeButton;

    public HomeKantinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_kantin, container, false);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Get NIS from arguments
        if (getArguments() != null) {
            nis = getArguments().getString("NIS");
        }
        if (nis == null) {
            Toast.makeText(getContext(), "Sesi tidak valid", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), Login.class));
            if (getActivity() != null) getActivity().finish();
            return view;
        }

        // Initialize views
        headerText = view.findViewById(R.id.headerText);
        totalAmount = view.findViewById(R.id.totalAmount);
        keluarButton = view.findViewById(R.id.keluar);
        historyLayout = view.findViewById(R.id.historyLayout);
        exchangeButton = view.findViewById(R.id.exchangeButton);

        // Load kantin data
        loadKantinData();

        // Load transaction history
        loadTransactionHistory();

        // Set logout button listener
        keluarButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        // Set exchange button listener
        exchangeButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TukarRupiah.class);
            intent.putExtra("NIS", nis);
            startActivity(intent);
        });

        return view;
    }

    private void loadKantinData() {
        databaseReference.child("Users/Kantin").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    Long saldoKantin = snapshot.child("saldo_kantin").getValue(Long.class);
                    if (name != null) {
                        headerText.setText("Hello, " + name);
                    }
                    if (saldoKantin != null) {
                        totalAmount.setText(String.format("%,d", saldoKantin));
                    }
                } else {
                    Toast.makeText(getContext(), "Data kantin tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeKantinFragment", "Failed to load kantin data: " + error.getMessage());
                Toast.makeText(getContext(), "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactionHistory() {
        databaseReference.child("Transactions").orderByChild("user_nis").equalTo(nis)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyLayout.removeAllViews();
                        if (!snapshot.exists()) {
                            addHistoryItem("Tidak ada riwayat", "", 0);
                            return;
                        }
                        for (DataSnapshot transaction : snapshot.getChildren()) {
                            String userName = transaction.child("user_name").getValue(String.class);
                            String type = transaction.child("type").getValue(String.class);
                            Long amount = transaction.child("amount").getValue(Long.class);
                            Long timestamp = transaction.child("timestamp").getValue(Long.class);
                            if (userName != null && type != null && amount != null && timestamp != null) {
                                String date = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        .format(new java.util.Date(timestamp));
                                String displayText = type.equals("koin_to_rupiah") ? "Penukaran Rupiah" : "Penukaran Koin/Kupon";
                                addHistoryItem(userName + " - " + displayText, date, amount);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeKantinFragment", "Failed to load transactions: " + error.getMessage());
                        addHistoryItem("Gagal memuat riwayat", "", 0);
                    }
                });
    }

    private void addHistoryItem(String title, String date, long amount) {
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setBackgroundResource(R.drawable.box_white);
        itemLayout.setPadding(20, 20, 20, 20);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0)
        {{ setMargins(20, 10, 20, 10); }});

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(getContext());
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(0xFF000000);

        TextView dateView = new TextView(getContext());
        dateView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dateView.setText(date);
        dateView.setTextSize(14);
        dateView.setTextColor(0xFF757575);

        textLayout.addView(titleView);
        textLayout.addView(dateView);

        TextView amountView = new TextView(getContext());
        amountView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        amountView.setText(String.format("%,d", amount));
        amountView.setTextSize(18);
        amountView.setTextColor(getResources().getColor(R.color.orange));

        itemLayout.addView(textLayout);
        itemLayout.addView(amountView);

        historyLayout.addView(itemLayout);
    }
}
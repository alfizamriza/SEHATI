package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TukarRupiah extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private String nis;
    private TextView txtAmount, textTotalKoin;
    private Button btnExchange;
    private long selectedKoin = 0;
    private final long KOINS_PER_RUPIAH = 1000; // 1000 koin = Rp 5,000
    private final long RUPIAH_RATE = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tukar_rupiah);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Get NIS
        nis = getIntent().getStringExtra("NIS");
        if (nis == null) {
            Toast.makeText(this, "Sesi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        txtAmount = findViewById(R.id.txtAmount);
        textTotalKoin = findViewById(R.id.textTotalKoin);
        btnExchange = findViewById(R.id.btnExchange);
        ImageButton btnKembali = findViewById(R.id.btnKembali);
        Button[] exchangeButtons = {
                findViewById(R.id.btn50k),
                findViewById(R.id.btn100k),
                findViewById(R.id.btn200k),
                findViewById(R.id.btn500k),
                findViewById(R.id.btn1m),
                findViewById(R.id.btn2m),
                findViewById(R.id.btn5m),
                findViewById(R.id.btn10m)
        };

        // Load current saldo
        loadSaldoKantin();

        // Set back button listener
        btnKembali.setOnClickListener(v -> finish());

        // Set exchange buttons listeners
        long[] koinValues = {10000, 20000, 40000, 100000, 200000, 400000, 1000000, 2000000}; // Koin untuk Rp 50k, 100k, ...
        for (int i = 0; i < exchangeButtons.length; i++) {
            final long koin = koinValues[i];
            exchangeButtons[i].setOnClickListener(v -> {
                selectedKoin = koin;
                long rupiah = (selectedKoin / KOINS_PER_RUPIAH) * RUPIAH_RATE;
                txtAmount.setText(String.format("Rp %,d", rupiah));
            });
        }

        // Set exchange button listener
        btnExchange.setOnClickListener(v -> {
            if (selectedKoin == 0) {
                Toast.makeText(this, "Pilih jumlah untuk ditukar", Toast.LENGTH_SHORT).show();
                return;
            }
            performExchange();
        });

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadSaldoKantin() {
        databaseRef.child("Users/Kantin").child(nis).child("saldo_kantin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long saldo = snapshot.getValue(Long.class);
                        if (saldo != null) {
                            textTotalKoin.setText(String.format("%,d koin", saldo));
                            txtAmount.setText(String.format("Rp %,d", saldo * RUPIAH_RATE / KOINS_PER_RUPIAH));
                        } else {
                            textTotalKoin.setText("0 koin");
                            txtAmount.setText("Rp 0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TukarRupiah", "Failed to load saldo: " + error.getMessage());
                        Toast.makeText(TukarRupiah.this, "Gagal memuat saldo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performExchange() {
        btnExchange.setEnabled(false);
        databaseRef.child("Users/Kantin").child(nis).child("saldo_kantin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentSaldo = snapshot.getValue(Long.class);
                        if (currentSaldo == null) currentSaldo = 0L;
                        if (currentSaldo < selectedKoin) {
                            Toast.makeText(TukarRupiah.this, "Saldo koin tidak cukup", Toast.LENGTH_SHORT).show();
                            btnExchange.setEnabled(true);
                            return;
                        }

                        // Update saldo
                        long newSaldo = currentSaldo - selectedKoin;
                        databaseRef.child("Users/Kantin").child(nis).child("saldo_kantin").setValue(newSaldo);

                        // Record transaction
                        String transactionId = databaseRef.child("Transactions").push().getKey();
                        if (transactionId != null) {
                            Map<String, Object> transaction = new HashMap<>();
                            transaction.put("user_nis", nis);
                            transaction.put("user_name", "Kantin");
                            transaction.put("type", "koin_to_rupiah");
                            transaction.put("amount", (selectedKoin / KOINS_PER_RUPIAH) * RUPIAH_RATE);
                            transaction.put("timestamp", ServerValue.TIMESTAMP);
                            transaction.put("status", "completed");

                            databaseRef.child("Transactions").child(transactionId).setValue(transaction)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("TukarRupiah", "Transaction successful: " + selectedKoin + " koin");
                                        Intent intent = new Intent(TukarRupiah.this, BuktiTransaksiActivity.class);
                                        intent.putExtra("AMOUNT", (selectedKoin / KOINS_PER_RUPIAH) * RUPIAH_RATE);
                                        intent.putExtra("DATE", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
                                        intent.putExtra("TRANSACTION_ID", transactionId);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TukarRupiah", "Transaction failed: " + e.getMessage());
                                        Toast.makeText(TukarRupiah.this, "Penukaran gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        btnExchange.setEnabled(true);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TukarRupiah", "Failed to read saldo: " + error.getMessage());
                        Toast.makeText(TukarRupiah.this, "Gagal memproses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        btnExchange.setEnabled(true);
                    }
                });
    }
}
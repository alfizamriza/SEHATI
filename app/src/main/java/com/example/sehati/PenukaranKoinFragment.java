package com.example.sehati;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PenukaranKoinFragment extends Fragment {

    private DatabaseReference databaseRef;
    private String kantinNIS;
    private EditText editNIS, editKoin;
    private TextView studentNama;
    private Button btnSearch, btnExchange;
    private String targetNIS;
    private String targetNamaStudent;

    public PenukaranKoinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_penukaran_koin, container, false);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Get kantin NIS from arguments
        if (getArguments() != null) {
            kantinNIS = getArguments().getString("NIS");
        }
        if (kantinNIS == null && getContext() != null) {
            Toast.makeText(getContext(), "Sesi tidak valid", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize views
        editNIS = view.findViewById(R.id.editTextNIS);
        studentNama = view.findViewById(R.id.tvStudentName);
        editKoin = view.findViewById(R.id.editKoin);
        btnSearch = view.findViewById(R.id.buttonSearch);
        btnExchange = view.findViewById(R.id.buttonExchange);

        // Initially hide koin input and exchange button
        editKoin.setVisibility(View.GONE);
        btnExchange.setVisibility(View.GONE);
        studentNama.setVisibility(View.GONE);

        // Search student button listener
        btnSearch.setOnClickListener(v -> {
            String nisInput = editNIS.getText().toString().trim();
            if (nisInput.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), "Masukkan NIS siswa", Toast.LENGTH_SHORT).show();
                return;
            }
            searchStudent(nisInput);
        });

        // Exchange button listener
        btnExchange.setOnClickListener(v -> {
            String koinStr = editKoin.getText().toString().trim();
            if (koinStr.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), "Masukkan jumlah koin", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long koin = Long.parseLong(koinStr);
                if (koin <= 0 && getContext() != null) {
                    Toast.makeText(getContext(), "Jumlah koin harus lebih dari 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                performExchange(koin);
            } catch (NumberFormatException e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Jumlah koin tidak valid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void searchStudent(String nis) {
        btnSearch.setEnabled(false);
        databaseRef.child("Users/Students").child(nis).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                btnSearch.setEnabled(true);
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && getContext() != null) {
                        targetNIS = nis;
                        targetNamaStudent = name;
                        studentNama.setText("Nama: " + name);
                        studentNama.setVisibility(View.VISIBLE);
                        editKoin.setVisibility(View.VISIBLE);
                        btnExchange.setVisibility(View.VISIBLE);
                        editNIS.setEnabled(false); // Disable NIS input after success
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Data siswa tidak lengkap", Toast.LENGTH_SHORT).show();
                    }
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Siswa tidak ditemukan", Toast.LENGTH_SHORT).show();
                    resetForm();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSearch.setEnabled(true);
                Log.e("PenukaranKoinFragment", "Search failed: " + error.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal mencari siswa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void performExchange(long koin) {
        btnExchange.setEnabled(false);
        databaseRef.child("Users/Students").child(targetNIS).child("koin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentKoin = snapshot.getValue(Long.class);
                        if (currentKoin == null) currentKoin = 0L;
                        if (currentKoin < koin && getContext() != null) {
                            Toast.makeText(getContext(), "Koin siswa tidak cukup", Toast.LENGTH_SHORT).show();
                            btnExchange.setEnabled(true);
                            return;
                        }

                        // Update student koin
                        long newStudentKoin = currentKoin - koin;
                        databaseRef.child("Users/Students").child(targetNIS).child("koin").setValue(newStudentKoin);

                        // Update kantin saldo
                        databaseRef.child("Users/Kantin").child(kantinNIS).child("saldo_kantin")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Long currentSaldo = snapshot.getValue(Long.class);
                                        if (currentSaldo == null) currentSaldo = 0L;
                                        long newSaldo = currentSaldo + koin;
                                        databaseRef.child("Users/Kantin").child(kantinNIS).child("saldo_kantin").setValue(newSaldo);

                                        // Record transaction
                                        String transactionId = databaseRef.child("Transactions").push().getKey();
                                        if (transactionId != null) {
                                            Map<String, Object> transaction = new HashMap<>();
                                            transaction.put("user_nis", kantinNIS);
                                            transaction.put("user_name", "Kantin");
                                            transaction.put("target_nis", targetNIS);
                                            transaction.put("target_name", targetNamaStudent);
                                            transaction.put("type", "koin_to_item");
                                            transaction.put("amount", koin);
                                            transaction.put("timestamp", ServerValue.TIMESTAMP);
                                            transaction.put("status", "completed");

                                            databaseRef.child("Transactions").child(transactionId).setValue(transaction)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("PenukaranKoinFragment", "Exchange successful: " + koin + " koin");
                                                        if (getContext() != null) {
                                                            Toast.makeText(getContext(), "Penukaran berhasil", Toast.LENGTH_SHORT).show();
                                                        }
                                                        resetForm();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("PenukaranKoinFragment", "Transaction failed: " + e.getMessage());
                                                        if (getContext() != null) {
                                                            Toast.makeText(getContext(), "Penukaran gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                        btnExchange.setEnabled(true);
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("PenukaranKoinFragment", "Failed to update saldo: " + error.getMessage());
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Gagal memproses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        btnExchange.setEnabled(true);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PenukaranKoinFragment", "Failed to read koin: " + error.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Gagal memproses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        btnExchange.setEnabled(true);
                    }
                });
    }

    private void resetForm() {
        editNIS.setText("");
        editNIS.setEnabled(true);
        studentNama.setVisibility(View.GONE);
        editKoin.setText("");
        editKoin.setVisibility(View.GONE);
        btnExchange.setVisibility(View.GONE);
        btnExchange.setEnabled(true);
        targetNIS = null;
        targetNamaStudent = null;
    }
}
package com.example.sehati;

import android.app.AlertDialog;
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

public class PenukaranKuponFragment extends Fragment {

    private DatabaseReference databaseRef;
    private String kantinNIS;
    private EditText editNIS, editKuponAmount;
    private TextView tvStudentName, tvKuponCount;
    private Button btnSearch, btnExchange;
    private String targetNIS, targetNamaStudent;
    private long targetKupon;

    public PenukaranKuponFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_penukaran_kupon, container, false);

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
        tvStudentName = view.findViewById(R.id.tvStudentName);
        tvKuponCount = view.findViewById(R.id.tvKuponCount);
        editKuponAmount = view.findViewById(R.id.editKuponAmount);
        btnSearch = view.findViewById(R.id.buttonSearch);
        btnExchange = view.findViewById(R.id.buttonExchange);

        // Initially hide result views
        tvStudentName.setVisibility(View.GONE);
        tvKuponCount.setVisibility(View.GONE);
        editKuponAmount.setVisibility(View.GONE);
        btnExchange.setVisibility(View.GONE);

        // Search button listener
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
            String kuponStr = editKuponAmount.getText().toString().trim();
            if (kuponStr.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), "Masukkan jumlah kupon", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                long kupon = Long.parseLong(kuponStr);
                if (kupon <= 0 && getContext() != null) {
                    Toast.makeText(getContext(), "Jumlah kupon harus lebih dari 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (kupon > targetKupon && getContext() != null) {
                    Toast.makeText(getContext(), "Kupon siswa tidak cukup", Toast.LENGTH_SHORT).show();
                    return;
                }
                showConfirmationDialog(kupon);
            } catch (NumberFormatException e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Jumlah kupon tidak valid", Toast.LENGTH_SHORT).show();
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
                    Long kupon = snapshot.child("kupon").getValue(Long.class);
                    if (name != null && getContext() != null) {
                        targetNIS = nis;
                        targetNamaStudent = name;
                        targetKupon = kupon != null ? kupon : 0;
                        tvStudentName.setText("Nama: " + name);
                        tvKuponCount.setText("Kupon: " + targetKupon);
                        tvStudentName.setVisibility(View.VISIBLE);
                        tvKuponCount.setVisibility(View.VISIBLE);
                        editKuponAmount.setVisibility(View.VISIBLE);
                        btnExchange.setVisibility(View.VISIBLE);
                        editNIS.setEnabled(false);
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Data siswa tidak lengkap", Toast.LENGTH_SHORT).show();
                        resetForm();
                    }
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Siswa tidak ditemukan", Toast.LENGTH_SHORT).show();
                    resetForm();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSearch.setEnabled(true);
                Log.e("PenukaranKuponFragment", "Search failed: " + error.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal mencari siswa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showConfirmationDialog(long kupon) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Konfirmasi Penukaran")
                .setMessage("Tukar " + kupon + " kupon untuk " + targetNamaStudent + "?")
                .setPositiveButton("Ya", (dialog, which) -> performExchange(kupon))
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void performExchange(long kupon) {
        btnExchange.setEnabled(false);
        databaseRef.child("Users/Students").child(targetNIS).child("kupon")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentKupon = snapshot.getValue(Long.class);
                        if (currentKupon == null) currentKupon = 0L;
                        if (currentKupon < kupon && getContext() != null) {
                            Toast.makeText(getContext(), "Kupon siswa tidak cukup", Toast.LENGTH_SHORT).show();
                            btnExchange.setEnabled(true);
                            return;
                        }

                        // Update student kupon
                        long newKupon = currentKupon - kupon;
                        databaseRef.child("Users/Students").child(targetNIS).child("kupon").setValue(newKupon);

                        // Update kantin saldo (1 kupon = 100 koin)
                        long koinReward = kupon * 100;
                        databaseRef.child("Users/Kantin").child(kantinNIS).child("saldo_kantin")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Long currentSaldo = snapshot.getValue(Long.class);
                                        if (currentSaldo == null) currentSaldo = 0L;
                                        long newSaldo = currentSaldo + koinReward;
                                        databaseRef.child("Users/Kantin").child(kantinNIS).child("saldo_kantin").setValue(newSaldo);

                                        // Record transaction
                                        String transactionId = databaseRef.child("Transactions").push().getKey();
                                        if (transactionId != null) {
                                            Map<String, Object> transaction = new HashMap<>();
                                            transaction.put("user_nis", kantinNIS);
                                            transaction.put("user_name", "Kantin");
                                            transaction.put("target_nis", targetNIS);
                                            transaction.put("target_name", targetNamaStudent);
                                            transaction.put("type", "kupon_exchange");
                                            transaction.put("amount", kupon);
                                            transaction.put("koin_reward", koinReward);
                                            transaction.put("timestamp", ServerValue.TIMESTAMP);
                                            transaction.put("status", "completed");

                                            databaseRef.child("Transactions").child(transactionId).setValue(transaction)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("PenukaranKuponFragment", "Exchange successful: " + kupon + " kupon");
                                                        if (getContext() != null) {
                                                            Toast.makeText(getContext(), "Penukaran kupon berhasil", Toast.LENGTH_SHORT).show();
                                                        }
                                                        resetForm();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("PenukaranKuponFragment", "Transaction failed: " + e.getMessage());
                                                        if (getContext() != null) {
                                                            Toast.makeText(getContext(), "Penukaran gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                        btnExchange.setEnabled(true);
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("PenukaranKuponFragment", "Failed to update saldo: " + error.getMessage());
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Gagal memproses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        btnExchange.setEnabled(true);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PenukaranKuponFragment", "Failed to read kupon: " + error.getMessage());
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
        tvStudentName.setVisibility(View.GONE);
        tvKuponCount.setVisibility(View.GONE);
        editKuponAmount.setText("");
        editKuponAmount.setVisibility(View.GONE);
        btnExchange.setVisibility(View.GONE);
        btnExchange.setEnabled(true);
        targetNIS = null;
        targetNamaStudent = null;
        targetKupon = 0;
    }
}
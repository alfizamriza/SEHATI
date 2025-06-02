package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BuktiTransaksiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bukti_transaksi);

        // Initialize views
        TextView txtAmount = findViewById(R.id.txtAmount);
        TextView txtDate = findViewById(R.id.txtDate);
        TextView txtTransactionId = findViewById(R.id.txtTransactionId);
        Button btnKembali = findViewById(R.id.btnKembali);

        // Get data from intent
        long amount = getIntent().getLongExtra("AMOUNT", 0);
        String date = getIntent().getStringExtra("DATE");
        String transactionId = getIntent().getStringExtra("TRANSACTION_ID");

        // Set data
        txtAmount.setText(String.format("Rp %,d", amount));
        txtDate.setText(date != null ? date : "-");
        txtTransactionId.setText(transactionId != null ? transactionId : "-");

        // Set back button listener
        btnKembali.setOnClickListener(v -> {
            Intent intent = new Intent(this, Kantin.class);
            intent.putExtra("NIS", getIntent().getStringExtra("NIS"));
            startActivity(intent);
            finish();
        });

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
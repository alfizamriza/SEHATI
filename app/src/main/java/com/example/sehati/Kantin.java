package com.example.sehati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class Kantin extends AppCompatActivity {

    private String nis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kantin);

        // Get NIS from Intent or SharedPreferences
        nis = getIntent().getStringExtra("NIS");
        if (nis == null) {
            SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            nis = prefs.getString("NIS", null);
        }

        if (nis == null) {
            Log.e("KantinActivity", "NIS is null, redirecting to MainActivity");
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Set up window insets
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_kantin);
        if (bottomNavigationView == null) {
            Log.e("KantinActivity", "BottomNavigationView not found");
            Toast.makeText(this, "Gagal memuat navigasi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load default fragment with NIS
        loadFragment(new HomeKantinFragment(), nis);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.menu_home_kantin) {
                    selectedFragment = new HomeKantinFragment();
                } else if (itemId == R.id.menu_penukaran_koin) {
                    selectedFragment = new PenukaranKoinFragment();
                } else if (itemId == R.id.menu_penukaran_kupon) {
                    selectedFragment = new PenukaranKuponFragment();
                } else {
                    Log.w("KantinActivity", "Unknown menu item selected: " + itemId);
                    return false;
                }

                // Load the selected fragment with NIS
                if (selectedFragment != null) {
                    loadFragment(selectedFragment, nis);
                }
                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment, String nis) {
        if (fragment != null) {
            try {
                // Pass NIS to fragment
                Bundle bundle = new Bundle();
                bundle.putString("NIS", nis);
                fragment.setArguments(bundle);

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
                Log.d("KantinActivity", "Loaded fragment: " + fragment.getClass().getSimpleName() + " with NIS: " + nis);
            } catch (Exception e) {
                Log.e("KantinActivity", "Error loading fragment: " + e.getMessage(), e);
                Toast.makeText(this, "Gagal memuat halaman: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("KantinActivity", "Fragment is null");
            Toast.makeText(this, "Gagal memuat halaman", Toast.LENGTH_SHORT).show();
        }
    }
}
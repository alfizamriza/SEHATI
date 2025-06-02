package com.example.sehati;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

public class Guru extends AppCompatActivity {

    private String nis;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guru);

        // Find the root view
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Get NIS and password from Intent
        Intent intent = getIntent();
        nis = intent.getStringExtra("NIS");
        password = intent.getStringExtra("PASSWORD");

        // Check if NIS is valid
        if (nis == null || nis.isEmpty()) {
            finish(); // Close activity if NIS is invalid
            return;
        }

        // Create HomeGuruFragment with NIS and password
        HomeGuruFragment homeGuruFragment = new HomeGuruFragment();
        Bundle bundle = new Bundle();
        bundle.putString("NIS", nis);
        bundle.putString("PASSWORD", password);
        homeGuruFragment.setArguments(bundle);

        // Load the default fragment
        loadFragment(homeGuruFragment);

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_guru);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                Bundle fragmentBundle = new Bundle();
                fragmentBundle.putString("NIS", nis);
                fragmentBundle.putString("PASSWORD", password);

                int itemId = item.getItemId();

                if (itemId == R.id.menu_home_guru) {
                    selectedFragment = new HomeGuruFragment();
                } else if (itemId == R.id.menu_absensi) {
                    selectedFragment = new AbsenFragment();
                } else if (itemId == R.id.menu_laporan) {
                    selectedFragment = new PelaporanFragment();
                } else {
                    return false;
                }

                // Apply bundle to selected fragment
                if (selectedFragment != null) {
                    selectedFragment.setArguments(fragmentBundle);
                    loadFragment(selectedFragment);
                }
                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
}
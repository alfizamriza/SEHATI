package com.example.sehati;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class kuponMuridFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String nis;
    private LinearLayout couponContainer;

    public kuponMuridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (getArguments() != null) {
            nis = getArguments().getString("NIS");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kupon_murid, container, false);

        couponContainer = view.findViewById(R.id.coupon_container);

        if (nis == null) {
            Toast.makeText(getContext(), "Data siswa tidak valid", Toast.LENGTH_SHORT).show();
            return view;
        }

        loadCoupons();

        return view;
    }

    private void loadCoupons() {
        databaseReference.child("Users").child("Students").child(nis).child("coupons")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        couponContainer.removeAllViews();

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        String today = sdf.format(new Date());

                        for (DataSnapshot couponSnapshot : snapshot.getChildren()) {
                            Long value = couponSnapshot.child("value").getValue(Long.class);
                            String code = couponSnapshot.child("code").getValue(String.class);
                            String expiry = couponSnapshot.child("expiry").getValue(String.class);

                            if (value == null || code == null || expiry == null) {
                                continue;
                            }

                            // Skip expired coupons
                            try {
                                if (sdf.parse(expiry).before(sdf.parse(today))) {
                                    continue;
                                }
                            } catch (Exception e) {
                                Log.e("kuponMuridFragment", "Error parsing date: " + e.getMessage());
                                continue;
                            }

                            addCouponCard(value, code, expiry);
                        }

                        if (couponContainer.getChildCount() == 0) {
                            TextView noCoupons = new TextView(getContext());
                            noCoupons.setText("Tidak ada kupon tersedia");
                            noCoupons.setTextSize(16);
                            noCoupons.setTextColor(getResources().getColor(android.R.color.black));
                            noCoupons.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            couponContainer.addView(noCoupons);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("kuponMuridFragment", "Failed to load coupons: " + error.getMessage());
                        Toast.makeText(getContext(), "Gagal memuat kupon: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addCouponCard(long value, String code, String expiry) {
        RelativeLayout card = new RelativeLayout(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 16, 0, 0);
        card.setLayoutParams(cardParams);
        card.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        card.setPadding(16, 16, 16, 16);
        card.setElevation(4);

        TextView tvCoupon = new TextView(getContext());
        tvCoupon.setId(View.generateViewId());
        tvCoupon.setText("KUPON");
        tvCoupon.setTextColor(getResources().getColor(android.R.color.white));
        tvCoupon.setTextSize(24);
        tvCoupon.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams couponParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        tvCoupon.setLayoutParams(couponParams);

        TextView tvValue = new TextView(getContext());
        tvValue.setId(View.generateViewId());
        tvValue.setText(String.format(Locale.US, "Rp %,d", value));
        tvValue.setTextColor(getResources().getColor(android.R.color.white));
        tvValue.setTextSize(20);
        RelativeLayout.LayoutParams valueParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        valueParams.addRule(RelativeLayout.BELOW, tvCoupon.getId());
        valueParams.setMargins(0, 8, 0, 0);
        tvValue.setLayoutParams(valueParams);

        TextView tvExpiry = new TextView(getContext());
        tvExpiry.setText("Tempo " + expiry);
        tvExpiry.setTextColor(getResources().getColor(android.R.color.white));
        tvExpiry.setTextSize(14);
        RelativeLayout.LayoutParams expiryParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        expiryParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        tvExpiry.setLayoutParams(expiryParams);

        TextView tvCode = new TextView(getContext());
        tvCode.setText(code);
        tvCode.setTextColor(getResources().getColor(android.R.color.white));
        tvCode.setTextSize(14);
        RelativeLayout.LayoutParams codeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        codeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        codeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        tvCode.setLayoutParams(codeParams);

        card.addView(tvCoupon);
        card.addView(tvValue);
        card.addView(tvExpiry);
        card.addView(tvCode);
        couponContainer.addView(card);
    }
}
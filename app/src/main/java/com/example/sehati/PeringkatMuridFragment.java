package com.example.sehati;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PeringkatMuridFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String nis;
    private RecyclerView recyclerView;
    private TextView tvNoRankings;
    private ClassRankingAdapter adapter;

    public PeringkatMuridFragment() {
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
        View view = inflater.inflate(R.layout.fragment_peringkat_murid, container, false);

        recyclerView = view.findViewById(R.id.rv_rankings);
        tvNoRankings = view.findViewById(R.id.tv_no_rankings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassRankingAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        if (nis == null) {
            tvNoRankings.setText("Data siswa tidak valid");
            tvNoRankings.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return view;
        }

        loadClassRankings();

        return view;
    }

    private void loadClassRankings() {
        String weekId = new SimpleDateFormat("yyyy-WW", Locale.US).format(new Date());
        Log.d("PeringkatMuridFragment", "Loading rankings for weekId: " + weekId);
        databaseReference.child("ClassRankings").child(weekId).child("classes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ClassRanking> rankings = new ArrayList<>();
                        boolean allZero = true;
                        Log.d("PeringkatMuridFragment", "Snapshot exists: " + snapshot.exists());

                        for (DataSnapshot classSnapshot : snapshot.getChildren()) {
                            String className = classSnapshot.getKey();
                            Long totalKoin = classSnapshot.child("total_koin").getValue(Long.class);
                            Log.d("PeringkatMuridFragment", "Class: " + className + ", Koin: " + totalKoin);
                            if (className != null && totalKoin != null) {
                                rankings.add(new ClassRanking(className, totalKoin));
                                if (totalKoin > 0) {
                                    allZero = false;
                                }
                            }
                        }

                        if (rankings.isEmpty()) {
                            // Tampilkan kelas default
                            String[] defaultClasses = {"X Asing", "X Banda Aceh", "X Pidie", "X Lhokseumawe"};
                            for (int i = 0; i < defaultClasses.length; i++) {
                                ClassRanking ranking = new ClassRanking(defaultClasses[i], 0);
                                ranking.rank = i + 1;
                                rankings.add(ranking);
                            }
                            adapter.setRankings(rankings);
                            tvNoRankings.setText("Belum ada koin minggu ini");
                            tvNoRankings.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.VISIBLE);
                            Log.d("PeringkatMuridFragment", "Loaded default rankings");
                            return;
                        }

                        if (allZero) {
                            // Tetap tampilkan rankings meskipun semua koin 0
                            Collections.sort(rankings, (r1, r2) -> Long.compare(r2.totalKoin, r1.totalKoin));
                            for (int i = 0; i < rankings.size(); i++) {
                                rankings.get(i).rank = i + 1;
                            }
                            adapter.setRankings(rankings);
                            tvNoRankings.setText("Belum ada koin minggu ini");
                            tvNoRankings.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.VISIBLE);
                            Log.d("PeringkatMuridFragment", "All koin zero");
                            return;
                        }

                        // Normal case: sort and display
                        Collections.sort(rankings, (r1, r2) -> Long.compare(r2.totalKoin, r1.totalKoin));
                        for (int i = 0; i < rankings.size(); i++) {
                            rankings.get(i).rank = i + 1;
                        }
                        adapter.setRankings(rankings);
                        tvNoRankings.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        Log.d("PeringkatMuridFragment", "Loaded rankings, size: " + rankings.size());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PeringkatMuridFragment", "Failed to load rankings: " + error.getMessage());
                        tvNoRankings.setText("Gagal memuat peringkat: " + error.getMessage());
                        tvNoRankings.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
    }

    private static class ClassRanking {
        String className;
        long totalKoin;
        int rank;

        ClassRanking(String className, long totalKoin) {
            this.className = className;
            this.totalKoin = totalKoin;
        }
    }

    private static class ClassRankingAdapter extends RecyclerView.Adapter<ClassRankingAdapter.ViewHolder> {
        private List<ClassRanking> rankings;

        ClassRankingAdapter(List<ClassRanking> rankings) {
            this.rankings = rankings;
        }

        void setRankings(List<ClassRanking> rankings) {
            this.rankings = rankings;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_class_ranking, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClassRanking ranking = rankings.get(position);
            holder.tvRank.setText(String.format(Locale.US, "No %d", ranking.rank));
            holder.tvClassName.setText(ranking.className);
            holder.tvKoin.setText(String.format(Locale.US, "%,d", ranking.totalKoin));
            holder.itemView.setBackgroundResource(
                    ranking.rank == 1 ? R.drawable.gradasi_oren : R.drawable.bg_gradasi);
        }

        @Override
        public int getItemCount() {
            return rankings.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvClassName, tvKoin;

            ViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_rank);
                tvClassName = itemView.findViewById(R.id.tv_class_name);
                tvKoin = itemView.findViewById(R.id.tv_koin);
            }
        }
    }
}
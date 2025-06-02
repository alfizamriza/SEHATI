package com.example.sehati;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ExchangeOptionAdapter extends RecyclerView.Adapter<ExchangeOptionAdapter.ViewHolder> {

    private final Context context;
    private final List<Long> koinValues;
    private final OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onOptionClick(long koin);
    }

    public ExchangeOptionAdapter(Context context, OnOptionClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.koinValues = new ArrayList<>();
        // Koin untuk Rp 50k, 100k, 200k, 500k, 1M, 2M, 5M, 10M
        long[] values = {10000, 20000, 40000, 100000, 200000, 400000, 1000000, 2000000};
        for (long value : values) {
            koinValues.add(value);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exchange_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        long koin = koinValues.get(position);
        long rupiah = (koin / 1000) * 5000; // 1000 koin = Rp 5,000
        holder.button.setText(String.format("Rp %,d", rupiah));
        holder.button.setOnClickListener(v -> listener.onOptionClick(koin));
    }

    @Override
    public int getItemCount() {
        return koinValues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Button button;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.btnOption);
        }
    }
}
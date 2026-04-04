package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ViewHolder> {
    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private final List<String> items = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private String selectedCategory = "Tất cả";

    public CategoryChipAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<String> categories) {
        items.clear();
        items.addAll(categories);
        notifyDataSetChanged();
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = items.get(position);
        boolean selected = category.equals(selectedCategory);
        holder.textChip.setText(category);
        holder.textChip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        holder.textChip.setTextColor(holder.itemView.getContext().getColor(selected ? R.color.white : R.color.coffee_500));
        holder.textChip.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textChip;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChip = itemView.findViewById(R.id.text_chip);
        }
    }
}

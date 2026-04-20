package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.CategoryShare;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class CategoryShareAdapter extends RecyclerView.Adapter<CategoryShareAdapter.ViewHolder> {
    private final List<CategoryShare> items = new ArrayList<>();

    public void submitList(List<CategoryShare> categoryShares) {
        items.clear();
        items.addAll(categoryShares);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_share, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryShare item = items.get(position);
        holder.textName.setText(item.getName());
        holder.textPercent.setText(item.getPercent() + "%");
        holder.viewColor.setBackground(UiUtils.roundedBackground(item.getColorHex(), 999f));
        holder.viewProgress.setBackground(UiUtils.roundedBackground(item.getColorHex(), 999f));
        holder.trackContainer.post(() -> {
            ViewGroup.LayoutParams params = holder.viewProgress.getLayoutParams();
            params.width = Math.max(6, (int) (holder.trackContainer.getWidth() * (item.getPercent() / 100f)));
            holder.viewProgress.setLayoutParams(params);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View viewColor;
        private final TextView textName;
        private final FrameLayout trackContainer;
        private final View viewProgress;
        private final TextView textPercent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColor = itemView.findViewById(R.id.view_color);
            textName = itemView.findViewById(R.id.text_name);
            viewProgress = itemView.findViewById(R.id.view_progress);
            trackContainer = (FrameLayout) viewProgress.getParent();
            textPercent = itemView.findViewById(R.id.text_percent);
        }
    }
}

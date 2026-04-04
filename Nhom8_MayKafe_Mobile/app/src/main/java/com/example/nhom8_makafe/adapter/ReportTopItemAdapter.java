package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.ReportItem;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportTopItemAdapter extends RecyclerView.Adapter<ReportTopItemAdapter.ViewHolder> {
    private final List<ReportItem> items = new ArrayList<>();

    public void submitList(List<ReportItem> reportItems) {
        items.clear();
        items.addAll(reportItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_top, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportItem item = items.get(position);
        int maxCount = items.isEmpty() ? 1 : items.get(0).getCount();
        float ratio = maxCount == 0 ? 0f : (float) item.getCount() / maxCount;

        UiUtils.bindThumbnail(holder.textThumbnail, holder.textThumbnail, item.getAssetLabel(), item.getAccentColorHex());
        holder.textName.setText(item.getName());
        holder.textSubtitle.setText(item.getCount() + " ly");
        holder.textRevenue.setText(FormatUtils.formatCurrency(item.getRevenue()));

        holder.textRankBadge.setVisibility(position < 3 ? View.VISIBLE : View.GONE);
        if (position == 0) {
            holder.textRankBadge.setBackgroundResource(R.drawable.bg_rank_gold);
            holder.textRankBadge.setText("1");
            holder.viewProgress.setBackground(UiUtils.roundedBackground("#F5A623", 999f));
        } else if (position == 1) {
            holder.textRankBadge.setBackgroundResource(R.drawable.bg_rank_silver);
            holder.textRankBadge.setText("2");
            holder.viewProgress.setBackground(UiUtils.roundedBackground("#C8956C", 999f));
        } else {
            holder.textRankBadge.setBackgroundResource(R.drawable.bg_rank_bronze);
            holder.textRankBadge.setText("3");
            holder.viewProgress.setBackground(UiUtils.roundedBackground("#6B3F2A", 999f));
        }
        if (position >= 3) {
            holder.viewProgress.setBackground(UiUtils.roundedBackground("#7A4A2D", 999f));
        }

        holder.trackContainer.post(() -> {
            ViewGroup.LayoutParams params = holder.viewProgress.getLayoutParams();
            params.width = Math.max(8, (int) (holder.trackContainer.getWidth() * ratio));
            holder.viewProgress.setLayoutParams(params);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textThumbnail;
        private final TextView textRankBadge;
        private final TextView textName;
        private final TextView textSubtitle;
        private final TextView textRevenue;
        private final FrameLayout trackContainer;
        private final View viewProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textThumbnail = itemView.findViewById(R.id.text_thumbnail);
            textRankBadge = itemView.findViewById(R.id.text_rank_badge);
            textName = itemView.findViewById(R.id.text_name);
            textSubtitle = itemView.findViewById(R.id.text_subtitle);
            textRevenue = itemView.findViewById(R.id.text_revenue);
            viewProgress = itemView.findViewById(R.id.view_progress);
            trackContainer = (FrameLayout) viewProgress.getParent();
        }
    }
}

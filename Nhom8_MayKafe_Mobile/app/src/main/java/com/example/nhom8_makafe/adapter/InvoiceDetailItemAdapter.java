package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.OrderItem;
import com.example.nhom8_makafe.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class InvoiceDetailItemAdapter extends RecyclerView.Adapter<InvoiceDetailItemAdapter.ViewHolder> {
    private final List<OrderItem> items = new ArrayList<>();

    public void submitList(List<OrderItem> orderItems) {
        items.clear();
        items.addAll(orderItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_detail_line, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);
        holder.textName.setText(item.getName());
        holder.textMeta.setText(FormatUtils.formatCurrency(item.getUnitPrice()) + " × " + item.getQuantity());
        holder.textTotal.setText(FormatUtils.formatCurrency(item.getLineTotal()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textMeta;
        private final TextView textTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_line_name);
            textMeta = itemView.findViewById(R.id.text_line_meta);
            textTotal = itemView.findViewById(R.id.text_line_total);
        }
    }
}

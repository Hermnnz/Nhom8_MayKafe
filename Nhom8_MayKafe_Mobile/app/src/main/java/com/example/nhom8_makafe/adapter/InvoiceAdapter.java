package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.OrderItem;
import com.example.nhom8_makafe.model.OrderStatus;
import com.example.nhom8_makafe.util.FormatUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {
    public interface InvoiceActionListener {
        void onShowDetail(Invoice invoice);

        void onPrint(Invoice invoice);
    }

    private final List<Invoice> items = new ArrayList<>();
    private final InvoiceActionListener listener;
    private String expandedInvoiceId;

    public InvoiceAdapter(InvoiceActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Invoice> invoices) {
        items.clear();
        items.addAll(invoices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invoice invoice = items.get(position);
        boolean expanded = invoice.getId().equals(expandedInvoiceId);
        List<OrderItem> invoiceItems = invoice.getItems();
        int invoiceTotal = resolveInvoiceTotal(invoiceItems, invoice.getTotal());
        String paymentMethod = invoice.getPaymentMethodLabel();
        boolean hasPaymentMethod = paymentMethod != null && !paymentMethod.trim().isEmpty();
        String note = invoice.getNote();
        boolean hasNote = note != null && !note.trim().isEmpty();

        holder.textInvoiceId.setText(invoice.getId());
        holder.textMeta.setText(invoice.getTableNumber() + " \u2022 " + formatDisplayDate(invoice.getDate()) + " " + invoice.getTime());
        holder.textTotal.setText(formatInvoiceTotal(invoiceTotal));
        holder.textPreview.setText(buildPreview(invoiceItems));
        holder.textExpandedTotal.setText(formatInvoiceTotal(invoiceTotal));
        holder.textPaymentMethod.setVisibility(hasPaymentMethod ? View.VISIBLE : View.GONE);
        holder.textPaymentMethod.setText(hasPaymentMethod ? paymentMethod : "");
        holder.textNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        holder.textNote.setText(hasNote ? "Ghi ch\u00fa: " + note : "");
        bindStatus(holder, invoice.getStatus());

        holder.layoutExpanded.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.imageExpand.setImageResource(expanded ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);

        holder.lineItemAdapter.submitList(invoiceItems);

        View.OnClickListener toggleListener = v -> {
            expandedInvoiceId = expanded ? null : invoice.getId();
            notifyDataSetChanged();
        };
        holder.layoutInvoiceRoot.setOnClickListener(toggleListener);
        holder.imageExpand.setOnClickListener(toggleListener);
        holder.buttonDetail.setOnClickListener(v -> listener.onShowDetail(invoice));
        holder.buttonPrint.setOnClickListener(v -> listener.onPrint(invoice));
    }

    private void bindStatus(ViewHolder holder, OrderStatus status) {
        if (status == OrderStatus.CANCELLED) {
            holder.layoutStatusChip.setBackgroundResource(R.drawable.bg_status_cancelled);
            holder.imageStatus.setImageResource(R.drawable.ic_stat_cancelled);
            holder.imageStatus.setColorFilter(ContextCompat.getColor(holder.imageStatus.getContext(), R.color.danger));
            holder.textStatus.setTextColor(ContextCompat.getColor(holder.textStatus.getContext(), R.color.danger));
            holder.textStatus.setText("\u0110\u00e3 h\u1ee7y");
            return;
        }

        holder.layoutStatusChip.setBackgroundResource(R.drawable.bg_status_paid);
        holder.imageStatus.setImageResource(R.drawable.ic_stat_paid);
        holder.imageStatus.setColorFilter(ContextCompat.getColor(holder.imageStatus.getContext(), R.color.success));
        holder.textStatus.setTextColor(ContextCompat.getColor(holder.textStatus.getContext(), R.color.success));
        holder.textStatus.setText("\u0110\u00e3 thanh to\u00e1n");
    }

    private String buildPreview(List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(orderItems.size()).append(" m\u00f3n").append(" \u2022 ");
        for (int i = 0; i < orderItems.size(); i++) {
            builder.append(orderItems.get(i).getName());
            if (i < orderItems.size() - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private String formatDisplayDate(String isoDate) {
        if (isoDate == null) {
            return "";
        }
        String[] parts = isoDate.split("-");
        if (parts.length != 3) {
            return isoDate;
        }
        return parts[2] + "/" + parts[1] + "/" + parts[0];
    }

    private String formatInvoiceTotal(int total) {
        String formatted = FormatUtils.formatCurrency(total).replace("\u0111", "\u20ab");
        int symbolIndex = formatted.lastIndexOf('\u20ab');
        if (symbolIndex < 0) {
            return formatted;
        }
        return formatted.substring(0, symbolIndex).trim() + " \u20ab";
    }

    private int resolveInvoiceTotal(List<OrderItem> invoiceItems, int storedTotal) {
        if (storedTotal > 0) {
            return storedTotal;
        }

        int calculatedTotal = 0;
        for (int i = 0; i < invoiceItems.size(); i++) {
            calculatedTotal += invoiceItems.get(i).getLineTotal();
        }
        return calculatedTotal;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layoutInvoiceRoot;
        private final TextView textInvoiceId;
        private final LinearLayout layoutStatusChip;
        private final ImageView imageStatus;
        private final TextView textStatus;
        private final TextView textMeta;
        private final TextView textTotal;
        private final TextView textPaymentMethod;
        private final TextView textPreview;
        private final ImageView imageExpand;
        private final LinearLayout layoutExpanded;
        private final RecyclerView recyclerItems;
        private final TextView textNote;
        private final TextView textExpandedTotal;
        private final MaterialButton buttonPrint;
        private final MaterialButton buttonDetail;
        private final InvoiceLineItemAdapter lineItemAdapter;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutInvoiceRoot = itemView.findViewById(R.id.layout_invoice_root);
            textInvoiceId = itemView.findViewById(R.id.text_invoice_id);
            layoutStatusChip = itemView.findViewById(R.id.layout_status_chip);
            imageStatus = itemView.findViewById(R.id.image_status);
            textStatus = itemView.findViewById(R.id.text_status);
            textMeta = itemView.findViewById(R.id.text_meta);
            textTotal = itemView.findViewById(R.id.text_total);
            textPaymentMethod = itemView.findViewById(R.id.text_payment_method);
            textPreview = itemView.findViewById(R.id.text_preview);
            imageExpand = itemView.findViewById(R.id.image_expand);
            layoutExpanded = itemView.findViewById(R.id.layout_expanded);
            recyclerItems = itemView.findViewById(R.id.recycler_invoice_items);
            textNote = itemView.findViewById(R.id.text_note);
            textExpandedTotal = itemView.findViewById(R.id.text_expanded_total);
            buttonPrint = itemView.findViewById(R.id.button_print);
            buttonDetail = itemView.findViewById(R.id.button_detail);
            lineItemAdapter = new InvoiceLineItemAdapter();
            recyclerItems.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerItems.setAdapter(lineItemAdapter);
        }
    }
}

package com.example.nhom8_makafe.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.UiUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.ViewHolder> {
    public interface CartItemListener {
        void onIncrease(CartItem item);

        void onDecrease(CartItem item);

        void onNoteChanged(CartItem item, String note);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final CartItemListener listener;

    public CartItemAdapter(CartItemListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CartItem> cartItems) {
        items.clear();
        items.addAll(cartItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);
        UiUtils.bindThumbnail(holder.textThumbnail, holder.textThumbnail, item.getAssetLabel(), item.getAccentColorHex());
        holder.textName.setText(item.getName());
        holder.textPrice.setText(FormatUtils.formatCurrency(item.getPrice()));
        holder.textQuantity.setText(String.valueOf(item.getQuantity()));
        holder.textTotal.setText(FormatUtils.formatCurrency(item.getPrice() * item.getQuantity()));
        holder.layoutNote.setVisibility(item.getNote() != null && !item.getNote().isEmpty() ? View.VISIBLE : View.GONE);
        if (holder.noteWatcher != null) {
            holder.editNote.removeTextChangedListener(holder.noteWatcher);
        }
        holder.editNote.setText(item.getNote());

        holder.buttonPlus.setOnClickListener(v -> listener.onIncrease(item));
        holder.buttonMinus.setOnClickListener(v -> listener.onDecrease(item));
        holder.buttonNote.setOnClickListener(v -> {
            holder.layoutNote.setVisibility(holder.layoutNote.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (holder.layoutNote.getVisibility() == View.VISIBLE) {
                holder.editNote.requestFocus();
            }
        });
        holder.noteWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                listener.onNoteChanged(item, s.toString().trim());
            }
        };
        holder.editNote.addTextChangedListener(holder.noteWatcher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textThumbnail;
        private final TextView textName;
        private final TextView textPrice;
        private final TextView textQuantity;
        private final TextView textTotal;
        private final ImageButton buttonNote;
        private final ImageButton buttonMinus;
        private final ImageButton buttonPlus;
        private final TextInputLayout layoutNote;
        private final TextInputEditText editNote;
        private TextWatcher noteWatcher;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textThumbnail = itemView.findViewById(R.id.text_thumbnail);
            textName = itemView.findViewById(R.id.text_name);
            textPrice = itemView.findViewById(R.id.text_price);
            textQuantity = itemView.findViewById(R.id.text_quantity);
            textTotal = itemView.findViewById(R.id.text_total);
            buttonNote = itemView.findViewById(R.id.button_note);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            buttonPlus = itemView.findViewById(R.id.button_plus);
            layoutNote = itemView.findViewById(R.id.layout_note);
            editNote = itemView.findViewById(R.id.edit_note);
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}

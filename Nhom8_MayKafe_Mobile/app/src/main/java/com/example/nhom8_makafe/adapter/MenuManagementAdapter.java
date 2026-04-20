package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuManagementAdapter extends RecyclerView.Adapter<MenuManagementAdapter.ViewHolder> {
    public interface MenuActionListener {
        void onEdit(Product product);

        void onDelete(Product product);

        void onToggle(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final MenuActionListener listener;

    public MenuManagementAdapter(MenuActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> products) {
        List<Product> sortedProducts = new ArrayList<>(products);
        Collections.sort(sortedProducts, (first, second) -> Long.compare(second.getId(), first.getId()));
        items.clear();
        items.addAll(sortedProducts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = items.get(position);
        ImageLoader.load(holder.imageProduct, product.getImageUrl());
        holder.textName.setText(product.getName());
        holder.textCategory.setText(product.getCategory());
        holder.textPrice.setText(FormatUtils.formatCurrency(product.getPrice()));
        holder.layoutRoot.setAlpha(product.isAvailable() ? 1f : 0.74f);

        holder.switchAvailable.setOnCheckedChangeListener(null);
        holder.switchAvailable.setChecked(product.isAvailable());
        holder.switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                listener.onToggle(product);
            }
        });

        holder.buttonEdit.setColorFilter(ContextCompat.getColor(holder.buttonEdit.getContext(), R.color.coffee_700));
        holder.buttonDelete.setColorFilter(ContextCompat.getColor(holder.buttonDelete.getContext(), R.color.danger));
        holder.buttonEdit.setOnClickListener(v -> listener.onEdit(product));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(product));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layoutRoot;
        private final ImageView imageProduct;
        private final TextView textName;
        private final TextView textCategory;
        private final TextView textPrice;
        private final SwitchCompat switchAvailable;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutRoot = itemView.findViewById(R.id.layout_root);
            imageProduct = itemView.findViewById(R.id.image_product);
            textName = itemView.findViewById(R.id.text_name);
            textCategory = itemView.findViewById(R.id.text_category);
            textPrice = itemView.findViewById(R.id.text_price);
            switchAvailable = itemView.findViewById(R.id.switch_available);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}

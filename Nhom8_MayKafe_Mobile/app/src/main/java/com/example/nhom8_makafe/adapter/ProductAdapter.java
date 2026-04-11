package com.example.nhom8_makafe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.util.ImageLoader;
import com.example.nhom8_makafe.util.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    public interface ProductActionListener {
        void onAdd(Product product);

        void onIncrease(Product product);

        void onDecrease(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final ProductActionListener listener;
    private final Map<Long, Integer> quantityMap = new HashMap<>();

    public ProductAdapter(ProductActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> products) {
        items.clear();
        items.addAll(products);
        notifyDataSetChanged();
    }

    public void setQuantityMap(Map<Long, Integer> quantities) {
        quantityMap.clear();
        quantityMap.putAll(quantities);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = items.get(position);
        int quantity = quantityMap.containsKey(product.getId()) ? quantityMap.get(product.getId()) : 0;

        bindProductImage(holder, product);
        holder.textProductName.setText(product.getName());
        holder.textProductCategory.setText(product.getCategory());
        holder.textProductPrice.setText(FormatUtils.formatCurrency(product.getPrice()));
        holder.textQuantity.setText(String.valueOf(quantity));
        holder.textQtyBadge.setText(String.valueOf(quantity));
        holder.textQtyBadge.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);
        holder.buttonAdd.setVisibility(quantity == 0 ? View.VISIBLE : View.GONE);
        holder.layoutQuantity.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);

        holder.buttonAdd.setOnClickListener(v -> listener.onAdd(product));
        holder.buttonPlus.setOnClickListener(v -> listener.onIncrease(product));
        holder.buttonMinus.setOnClickListener(v -> listener.onDecrease(product));
    }

    private void bindProductImage(@NonNull ViewHolder holder, @NonNull Product product) {
        String imageUrl = product.getImageUrl();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();

        if (hasImage) {
            holder.imageProduct.setVisibility(View.VISIBLE);
            holder.textThumbnail.setVisibility(View.GONE);
            holder.layoutThumbnail.setBackground(null);
            ImageLoader.load(holder.imageProduct, imageUrl);
            return;
        }

        holder.imageProduct.setImageDrawable(null);
        holder.imageProduct.setVisibility(View.GONE);
        holder.textThumbnail.setVisibility(View.VISIBLE);
        UiUtils.bindThumbnail(holder.layoutThumbnail, holder.textThumbnail, product.getAssetLabel(), product.getAccentColorHex());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View layoutThumbnail;
        private final ImageView imageProduct;
        private final TextView textThumbnail;
        private final TextView textQtyBadge;
        private final TextView textProductName;
        private final TextView textProductCategory;
        private final TextView textProductPrice;
        private final MaterialButton buttonAdd;
        private final LinearLayout layoutQuantity;
        private final TextView textQuantity;
        private final TextView buttonMinus;
        private final TextView buttonPlus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutThumbnail = itemView.findViewById(R.id.layout_thumbnail);
            imageProduct = itemView.findViewById(R.id.image_product);
            textThumbnail = itemView.findViewById(R.id.text_thumbnail);
            textQtyBadge = itemView.findViewById(R.id.text_qty_badge);
            textProductName = itemView.findViewById(R.id.text_product_name);
            textProductCategory = itemView.findViewById(R.id.text_product_category);
            textProductPrice = itemView.findViewById(R.id.text_product_price);
            buttonAdd = itemView.findViewById(R.id.button_add);
            layoutQuantity = itemView.findViewById(R.id.layout_quantity);
            textQuantity = itemView.findViewById(R.id.text_quantity);
            buttonMinus = itemView.findViewById(R.id.button_minus);
            buttonPlus = itemView.findViewById(R.id.button_plus);
        }
    }
}

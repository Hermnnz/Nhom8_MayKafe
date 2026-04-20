package com.example.nhom8_makafe.util;

import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nhom8_makafe.R;

public final class ImageLoader {
    private static final int DEFAULT_PLACEHOLDER = R.drawable.bg_muted_panel;

    private ImageLoader() {
    }

    public static void load(ImageView imageView, String imageUrl) {
        load(imageView, (Object) imageUrl, DEFAULT_PLACEHOLDER);
    }

    public static void load(ImageView imageView, String imageUrl, int placeholderResId) {
        load(imageView, (Object) imageUrl, placeholderResId);
    }

    public static void load(ImageView imageView, @Nullable Uri imageUri, int placeholderResId) {
        load(imageView, (Object) imageUri, placeholderResId);
    }

    private static void load(ImageView imageView, @Nullable Object imageSource, int placeholderResId) {
        Glide.with(imageView)
                .load(imageSource)
                .placeholder(placeholderResId)
                .fallback(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}

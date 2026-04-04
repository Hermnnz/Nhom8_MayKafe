package com.example.nhom8_makafe.util;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.nhom8_makafe.R;

public final class ImageLoader {
    private ImageLoader() {
    }

    public static void load(ImageView imageView, String imageUrl) {
        Glide.with(imageView)
                .load(imageUrl)
                .placeholder(R.drawable.bg_muted_panel)
                .error(R.drawable.bg_muted_panel)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}

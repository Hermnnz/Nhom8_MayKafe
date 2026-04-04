package com.example.nhom8_makafe.util;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nhom8_makafe.R;
import com.google.android.material.snackbar.Snackbar;

public final class FeedbackSnackbar {
    private static final long FEEDBACK_DURATION_MS = 2200L;
    private static final String OVERLAY_TAG = "print_feedback_overlay";

    private FeedbackSnackbar() {
    }

    public static void showPrinting(View anchor, String invoiceId) {
        Snackbar snackbar = Snackbar.make(anchor, "", Snackbar.LENGTH_SHORT);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.removeAllViews();

        View content = createFeedbackView(snackbarLayout, invoiceId);
        ImageView buttonClose = content.findViewById(R.id.button_feedback_close);
        buttonClose.setOnClickListener(v -> snackbar.dismiss());

        snackbarLayout.addView(content, 0);
        ViewGroup.LayoutParams rawParams = snackbarLayout.getLayoutParams();
        if (rawParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
            int horizontal = dp(anchor.getContext(), 16);
            int bottom = dp(anchor.getContext(), 12);
            params.leftMargin = horizontal;
            params.rightMargin = horizontal;
            params.bottomMargin = bottom;
            snackbarLayout.setLayoutParams(params);
        }
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        snackbar.show();
    }

    public static void showPrintingOverlay(FrameLayout container, String invoiceId) {
        View existing = container.findViewWithTag(OVERLAY_TAG);
        if (existing != null) {
            container.removeView(existing);
        }

        View content = createFeedbackView(container, invoiceId);
        content.setTag(OVERLAY_TAG);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = dp(container.getContext(), 16);
        params.gravity = Gravity.BOTTOM;
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.bottomMargin = dp(container.getContext(), 72);
        content.setLayoutParams(params);

        ImageView buttonClose = content.findViewById(R.id.button_feedback_close);
        buttonClose.setOnClickListener(v -> removeOverlay(container, content));

        container.addView(content);
        content.setAlpha(0f);
        content.setTranslationY(-dp(container.getContext(), 10));
        content.animate().alpha(1f).translationY(0f).setDuration(180).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> removeOverlay(container, content), FEEDBACK_DURATION_MS);
    }

    private static View createFeedbackView(ViewGroup parent, String invoiceId) {
        View content = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_print_feedback, parent, false);
        TextView textMessage = content.findViewById(R.id.text_feedback_message);
        textMessage.setText("Đang in hóa đơn " + invoiceId + "...");
        return content;
    }

    private static void removeOverlay(FrameLayout container, View content) {
        if (content.getParent() != container) {
            return;
        }
        content.animate()
                .alpha(0f)
                .translationY(-dp(container.getContext(), 10))
                .setDuration(160)
                .withEndAction(() -> {
                    if (content.getParent() == container) {
                        container.removeView(content);
                    }
                })
                .start();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

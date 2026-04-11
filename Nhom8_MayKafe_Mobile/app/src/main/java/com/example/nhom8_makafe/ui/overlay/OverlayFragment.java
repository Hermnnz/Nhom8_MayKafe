package com.example.nhom8_makafe.ui.overlay;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.nhom8_makafe.R;

public abstract class OverlayFragment extends Fragment {
    private static final int SCRIM_COLOR = 0x52000000;

    public void show(@NonNull FragmentManager fragmentManager, @Nullable String tag) {
        fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.overlay_container, this, tag)
                .commitAllowingStateLoss();
    }

    public void dismissAllowingStateLoss() {
        if (!isAdded()) {
            return;
        }
        getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .remove(this)
                .commitAllowingStateLoss();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(shouldDismissOnBackPress()) {
                    @Override
                    public void handleOnBackPressed() {
                        dismissAllowingStateLoss();
                    }
                }
        );
    }

    protected boolean shouldDismissOnBackPress() {
        return true;
    }

    @NonNull
    protected View createBottomSheetOverlay(@NonNull View contentView, boolean dismissOnOutsideTap) {
        FrameLayout overlayRoot = createOverlayRoot(dismissOnOutsideTap);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        overlayRoot.addView(contentView, layoutParams);
        consumeContentClicks(contentView);
        return overlayRoot;
    }

    @NonNull
    protected View createCenteredDialogOverlay(@NonNull View contentView,
                                               float widthRatio,
                                               int maxWidthDp,
                                               boolean dismissOnOutsideTap) {
        FrameLayout overlayRoot = createOverlayRoot(dismissOnOutsideTap);
        FrameLayout.LayoutParams layoutParams = createDialogLayoutParams(Gravity.CENTER, widthRatio, maxWidthDp, 0);
        overlayRoot.addView(contentView, layoutParams);
        consumeContentClicks(contentView);
        return overlayRoot;
    }

    @NonNull
    protected View createTopDialogOverlay(@NonNull View contentView,
                                          float widthRatio,
                                          int maxWidthDp,
                                          int topOffsetDp,
                                          boolean dismissOnOutsideTap) {
        FrameLayout overlayRoot = createOverlayRoot(dismissOnOutsideTap);
        FrameLayout.LayoutParams layoutParams = createDialogLayoutParams(
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                widthRatio,
                maxWidthDp,
                dp(topOffsetDp) + getSystemBarsInset().top
        );
        overlayRoot.addView(contentView, layoutParams);
        consumeContentClicks(contentView);
        return overlayRoot;
    }

    private FrameLayout createOverlayRoot(boolean dismissOnOutsideTap) {
        FrameLayout overlayRoot = new FrameLayout(requireContext());
        overlayRoot.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayRoot.setClickable(true);
        overlayRoot.setFocusable(true);
        overlayRoot.setBackgroundColor(SCRIM_COLOR);
        overlayRoot.setOnClickListener(v -> {
            if (dismissOnOutsideTap) {
                dismissAllowingStateLoss();
            }
        });
        return overlayRoot;
    }

    private FrameLayout.LayoutParams createDialogLayoutParams(int gravity,
                                                              float widthRatio,
                                                              int maxWidthDp,
                                                              int topMargin) {
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        int desiredWidth = Math.round(screenWidth * widthRatio);
        if (maxWidthDp > 0) {
            desiredWidth = Math.min(desiredWidth, dp(maxWidthDp));
        }
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                desiredWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                gravity
        );
        layoutParams.topMargin = topMargin;
        return layoutParams;
    }

    private Insets getSystemBarsInset() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(requireActivity().getWindow().getDecorView());
        if (insets == null) {
            return Insets.NONE;
        }
        return insets.getInsets(WindowInsetsCompat.Type.systemBars());
    }

    private void consumeContentClicks(@NonNull View contentView) {
        contentView.setClickable(true);
        contentView.setFocusable(true);
        contentView.setOnClickListener(v -> {
        });
    }

    protected int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}

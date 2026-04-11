package com.example.nhom8_makafe.ui.cart;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.databinding.DialogPaymentSuccessBinding;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

public class PaymentSuccessDialogFragment extends OverlayFragment {
    public static final String TAG = "payment_success_dialog";
    private static final long AUTO_DISMISS_DELAY_MS = 1800L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private DialogPaymentSuccessBinding binding;

    public static PaymentSuccessDialogFragment newInstance() {
        return new PaymentSuccessDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPaymentSuccessBinding.inflate(inflater, container, false);
        return createBottomSheetOverlay(binding.getRoot(), true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::dismissSafely, AUTO_DISMISS_DELAY_MS);
    }

    private void dismissSafely() {
        if (isAdded()) {
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}

package com.example.nhom8_makafe.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.databinding.DialogMenuDeleteBlockedBinding;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

public class MenuDeleteBlockedOverlayFragment extends OverlayFragment {
    public static final String TAG = "menu_delete_blocked_overlay";
    private static final String ARG_MESSAGE = "message";

    @Nullable
    private DialogMenuDeleteBlockedBinding binding;

    public static MenuDeleteBlockedOverlayFragment newInstance(@NonNull String message) {
        MenuDeleteBlockedOverlayFragment fragment = new MenuDeleteBlockedOverlayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogMenuDeleteBlockedBinding.inflate(inflater, container, false);
        return createCenteredDialogOverlay(binding.getRoot(), 0.86f, 420, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }
        String message = getArguments() == null ? "" : getArguments().getString(ARG_MESSAGE, "");
        binding.textDialogTitle.setText("Hiện không thể xóa món");
        binding.textDialogMessage.setText(
                message == null || message.trim().isEmpty()
                        ? "Món này đã có trong hóa đơn nên không thể xóa khỏi hệ thống."
                        : message.trim()
        );
        binding.buttonAcknowledge.setText("Đã hiểu");
        binding.buttonAcknowledge.setOnClickListener(v -> dismissAllowingStateLoss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

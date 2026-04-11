package com.example.nhom8_makafe.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nhom8_makafe.databinding.DialogAuthConfirmBinding;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

public class LogoutConfirmOverlayFragment extends OverlayFragment {
    public interface Listener {
        void onLogoutConfirmed();
    }

    public static final String TAG = "logout_confirm_overlay";
    private static final String ARG_DISPLAY_NAME = "display_name";

    @Nullable
    private DialogAuthConfirmBinding binding;

    public static LogoutConfirmOverlayFragment newInstance(@NonNull String displayName) {
        LogoutConfirmOverlayFragment fragment = new LogoutConfirmOverlayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DISPLAY_NAME, displayName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAuthConfirmBinding.inflate(inflater, container, false);
        return createCenteredDialogOverlay(binding.getRoot(), 0.86f, 420, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }
        String displayName = getArguments() == null ? "" : getArguments().getString(ARG_DISPLAY_NAME, "");
        binding.textDialogTitle.setText("\u0110\u0103ng xu\u1ea5t?");
        binding.textDialogMessage.setText(
                "B\u1ea1n c\u00f3 ch\u1eafc ch\u1eafn mu\u1ed1n \u0111\u0103ng xu\u1ea5t kh\u1ecfi t\u00e0i kho\u1ea3n " + displayName + "?"
        );
        binding.buttonCancel.setText("H\u1ee7y");
        binding.buttonConfirm.setText("\u0110\u0103ng xu\u1ea5t");
        binding.buttonCancel.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonConfirm.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            if (getActivity() instanceof Listener) {
                ((Listener) getActivity()).onLogoutConfirmed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.nhom8_makafe.ui.cart;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.adapter.CartItemAdapter;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.databinding.BottomSheetCartBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.FormatUtils;
import com.example.nhom8_makafe.ui.overlay.OverlayFragment;

import java.util.List;

public class CartBottomSheetDialogFragment extends OverlayFragment implements SessionManager.Observer {
    private BottomSheetCartBinding binding;
    private final SessionManager sessionManager = SessionManager.getInstance();
    private CartItemAdapter adapter;
    private int discountPercent = 0;
    private boolean updatingDiscountInput = false;
    private boolean isEditingNote = false;

    public static CartBottomSheetDialogFragment newInstance() {
        return new CartBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCartBinding.inflate(inflater, container, false);
        return createBottomSheetOverlay(binding.getRoot(), true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new CartItemAdapter(new CartItemAdapter.CartItemListener() {
            @Override
            public void onIncrease(CartItem item) {
                sessionManager.increaseItem(item.getProductId());
            }

            @Override
            public void onDecrease(CartItem item) {
                sessionManager.decreaseItem(item.getProductId());
                if (sessionManager.getCartCount() == 0) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onNoteChanged(CartItem item, String note) {
                isEditingNote = true;
                sessionManager.updateNote(item.getProductId(), note);
                isEditingNote = false;
            }
        });
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCart.setAdapter(adapter);
        binding.buttonClose.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonPrimaryAction.setOnClickListener(v -> openCheckoutSheet());
        binding.buttonDiscountUp.setOnClickListener(v -> updateDiscountBy(1));
        binding.buttonDiscountDown.setOnClickListener(v -> updateDiscountBy(-1));
        binding.editDiscount.addTextChangedListener(simpleWatcher(() -> {
            if (!updatingDiscountInput) {
                renderState();
            }
        }));
        sessionManager.addObserver(this);
        renderState();
    }

    private void renderState() {
        if (binding == null) {
            return;
        }
        List<CartItem> cartItems = sessionManager.getCartItems();
        adapter.submitList(cartItems);
        binding.textSheetTitle.setText("\u0110\u01a1n h\u00e0ng (" + sessionManager.getCartCount() + " m\u00f3n)");

        int subtotal = sessionManager.getCartSubtotal();
        discountPercent = parseInt(binding.editDiscount.getText() == null ? "" : binding.editDiscount.getText().toString());
        if (discountPercent < 0) {
            discountPercent = 0;
        }
        if (discountPercent > 100) {
            discountPercent = 100;
        }
        syncDiscountField(discountPercent);
        int discountAmount = Math.round(subtotal * discountPercent / 100f);
        int finalTotal = Math.max(0, subtotal - discountAmount);

        binding.textSubtotal.setText(FormatUtils.formatCurrency(subtotal));
        binding.layoutDiscountAmount.setVisibility(discountAmount > 0 ? View.VISIBLE : View.GONE);
        binding.textDiscountAmount.setText("-" + FormatUtils.formatCurrency(discountAmount));
        binding.textFinalTotal.setText(FormatUtils.formatCurrency(finalTotal));
        binding.buttonPrimaryAction.setEnabled(!cartItems.isEmpty());
        binding.buttonPrimaryAction.setAlpha(cartItems.isEmpty() ? 0.45f : 1f);
    }

    private void openCheckoutSheet() {
        if (binding == null || sessionManager.getCartItems().isEmpty()) {
            return;
        }
        if (getParentFragmentManager().findFragmentByTag("checkout_sheet") != null) {
            return;
        }
        CheckoutBottomSheetDialogFragment.newInstance(discountPercent)
                .show(getParentFragmentManager(), "checkout_sheet");
    }

    private void updateDiscountBy(int delta) {
        if (binding == null) {
            return;
        }
        int current = parseInt(binding.editDiscount.getText() == null ? "" : binding.editDiscount.getText().toString());
        int next = Math.max(0, Math.min(100, current + delta));
        setDiscountText(next);
    }

    private void setDiscountText(int value) {
        if (binding == null) {
            return;
        }
        updatingDiscountInput = true;
        binding.editDiscount.setText(String.valueOf(value));
        Editable editable = binding.editDiscount.getText();
        if (editable != null) {
            binding.editDiscount.setSelection(editable.length());
        }
        updatingDiscountInput = false;
        renderState();
    }

    private void syncDiscountField(int value) {
        if (binding == null || updatingDiscountInput) {
            return;
        }
        String currentValue = binding.editDiscount.getText() == null ? "" : binding.editDiscount.getText().toString().trim();
        String normalizedValue = value == 0 && currentValue.isEmpty() ? "" : String.valueOf(value);
        if (!currentValue.equals(normalizedValue)) {
            updatingDiscountInput = true;
            binding.editDiscount.setText(normalizedValue);
            Editable editable = binding.editDiscount.getText();
            if (editable != null) {
                binding.editDiscount.setSelection(editable.length());
            }
            updatingDiscountInput = false;
        }
    }

    private TextWatcher simpleWatcher(Runnable runnable) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                runnable.run();
            }
        };
    }

    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public void onSessionChanged(User user) {
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        if (binding == null) {
            return;
        }
        if (cartItems.isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }
        if (!isEditingNote) {
            renderState();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sessionManager.removeObserver(this);
        binding = null;
    }
}

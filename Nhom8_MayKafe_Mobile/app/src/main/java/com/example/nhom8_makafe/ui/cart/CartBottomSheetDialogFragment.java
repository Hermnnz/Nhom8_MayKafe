package com.example.nhom8_makafe.ui.cart;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nhom8_makafe.R;
import com.example.nhom8_makafe.adapter.CartItemAdapter;
import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.BottomSheetCartBinding;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.PaymentMethod;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.FormatUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class CartBottomSheetDialogFragment extends BottomSheetDialogFragment implements SessionManager.Observer {
    private BottomSheetCartBinding binding;
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ApiRepository apiRepository = ApiRepository.getInstance();
    private CartItemAdapter adapter;
    private boolean paymentStep = false;
    private PaymentMethod paymentMethod;
    private int discountPercent = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static CartBottomSheetDialogFragment newInstance() {
        return new CartBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> renderState());
        return dialog;
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
                sessionManager.updateNote(item.getProductId(), note);
            }
        });
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCart.setAdapter(adapter);
        binding.buttonClose.setOnClickListener(v -> dismissAllowingStateLoss());
        binding.buttonBack.setOnClickListener(v -> {
            paymentStep = false;
            renderState();
        });
        binding.buttonPrimaryAction.setOnClickListener(v -> {
            if (paymentStep) {
                completeOrder();
            } else {
                paymentStep = true;
                renderState();
            }
        });
        binding.cardPaymentCash.setOnClickListener(v -> {
            paymentMethod = PaymentMethod.CASH;
            renderState();
        });
        binding.cardPaymentQr.setOnClickListener(v -> {
            paymentMethod = PaymentMethod.QR;
            renderState();
        });
        binding.editDiscount.addTextChangedListener(simpleWatcher(this::renderState));
        binding.editCashGiven.addTextChangedListener(simpleWatcher(this::renderState));
        sessionManager.addObserver(this);
        renderState();
    }

    private void renderState() {
        if (binding == null) {
            return;
        }
        List<CartItem> cartItems = sessionManager.getCartItems();
        adapter.submitList(cartItems);

        if (!paymentStep) {
            binding.buttonBack.setVisibility(View.GONE);
            binding.textSheetTitle.setText("\u0110\u01a1n h\u00e0ng (" + sessionManager.getCartCount() + " m\u00f3n)");
            binding.layoutCartStep.setVisibility(View.VISIBLE);
            binding.layoutPaymentStep.setVisibility(View.GONE);
            binding.buttonPrimaryAction.setText("X\u00e1c nh\u1eadn \u0111\u01a1n h\u00e0ng");
        } else {
            binding.buttonBack.setVisibility(View.VISIBLE);
            binding.textSheetTitle.setText("Thanh to\u00e1n");
            binding.layoutCartStep.setVisibility(View.GONE);
            binding.layoutPaymentStep.setVisibility(View.VISIBLE);
            binding.buttonPrimaryAction.setText(paymentMethod == PaymentMethod.QR
                    ? "\u0110\u00e3 chuy\u1ec3n kho\u1ea3n \u2713"
                    : "X\u00e1c nh\u1eadn thanh to\u00e1n");
        }

        int subtotal = sessionManager.getCartSubtotal();
        discountPercent = parseInt(binding.editDiscount.getText() == null ? "" : binding.editDiscount.getText().toString());
        if (discountPercent < 0) {
            discountPercent = 0;
        }
        if (discountPercent > 100) {
            discountPercent = 100;
        }
        int discountAmount = Math.round(subtotal * discountPercent / 100f);
        int finalTotal = Math.max(0, subtotal - discountAmount);

        binding.textSubtotal.setText("T\u1ea1m t\u00ednh: " + FormatUtils.formatCurrency(subtotal));
        binding.textDiscountAmount.setVisibility(discountPercent > 0 ? View.VISIBLE : View.GONE);
        binding.textDiscountAmount.setText("Gi\u1ea3m gi\u00e1: -" + FormatUtils.formatCurrency(discountAmount));
        binding.textFinalTotal.setText("T\u1ed5ng c\u1ed9ng: " + FormatUtils.formatCurrency(finalTotal));
        binding.textPaymentTotal.setText(FormatUtils.formatCurrency(finalTotal));
        binding.textPaymentSummary.setText(sessionManager.getCartCount() + " m\u00f3n" + (discountPercent > 0 ? " \u2022 gi\u1ea3m " + discountPercent + "%" : ""));
        binding.textQrAmount.setText("S\u1ed1 ti\u1ec1n: " + FormatUtils.formatCurrency(finalTotal));

        boolean cashSelected = paymentMethod == PaymentMethod.CASH;
        boolean qrSelected = paymentMethod == PaymentMethod.QR;
        binding.cardPaymentCash.setStrokeColor(requireContext().getColor(cashSelected ? R.color.coffee_700 : R.color.coffee_200));
        binding.cardPaymentQr.setStrokeColor(requireContext().getColor(qrSelected ? R.color.coffee_700 : R.color.coffee_200));
        binding.cardPaymentCash.setCardBackgroundColor(requireContext().getColor(cashSelected ? R.color.coffee_100 : R.color.white));
        binding.cardPaymentQr.setCardBackgroundColor(requireContext().getColor(qrSelected ? R.color.coffee_100 : R.color.white));
        binding.layoutCashDetails.setVisibility(cashSelected ? View.VISIBLE : View.GONE);
        binding.layoutQrDetails.setVisibility(qrSelected ? View.VISIBLE : View.GONE);

        int cashGiven = parseInt(binding.editCashGiven.getText() == null ? "" : binding.editCashGiven.getText().toString());
        int change = cashGiven - finalTotal;
        boolean cashValid = !cashSelected || cashGiven >= finalTotal;
        binding.textChange.setVisibility(cashSelected && cashGiven > 0 ? View.VISIBLE : View.GONE);
        binding.textCashError.setVisibility(cashSelected && cashGiven > 0 && change < 0 ? View.VISIBLE : View.GONE);
        if (cashSelected && cashGiven > 0) {
            if (change >= 0) {
                binding.textChange.setText("Ti\u1ec1n th\u1eeba tr\u1ea3 l\u1ea1i: " + FormatUtils.formatCurrency(change));
                binding.textChange.setBackgroundResource(R.drawable.bg_success_pill);
                binding.textChange.setTextColor(requireContext().getColor(R.color.success));
            } else {
                binding.textChange.setText("C\u00f2n thi\u1ebfu: " + FormatUtils.formatCurrency(Math.abs(change)));
                binding.textChange.setBackgroundResource(R.drawable.bg_status_cancelled);
                binding.textChange.setTextColor(requireContext().getColor(R.color.danger));
            }
        }
        boolean actionEnabled = paymentStep ? paymentMethod != null && cashValid : !cartItems.isEmpty();
        binding.buttonPrimaryAction.setEnabled(actionEnabled);
        binding.buttonPrimaryAction.setAlpha(actionEnabled ? 1f : 0.45f);
    }

    private void completeOrder() {
        if (paymentMethod == null) {
            return;
        }
        int subtotal = sessionManager.getCartSubtotal();
        int discountAmount = Math.round(subtotal * discountPercent / 100f);
        int finalTotal = Math.max(0, subtotal - discountAmount);
        int cashGiven = parseInt(binding.editCashGiven.getText() == null ? "" : binding.editCashGiven.getText().toString());
        if (paymentMethod == PaymentMethod.CASH && cashGiven < finalTotal) {
            renderState();
            return;
        }
        User user = sessionManager.getCurrentUser();
        if (user == null) {
            dismissAllowingStateLoss();
            return;
        }

        binding.buttonPrimaryAction.setEnabled(false);
        apiRepository.checkout("B\u00e0n m\u1edbi", discountPercent, paymentMethod, cashGiven, sessionManager.getCartItems(), new ApiCallback<Invoice>() {
            @Override
            public void onSuccess(Invoice data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                sessionManager.clearCart();
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutSuccess.setVisibility(View.VISIBLE);
                handler.postDelayed(CartBottomSheetDialogFragment.this::dismissAllowingStateLoss, 1800);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                binding.buttonPrimaryAction.setEnabled(true);
                binding.textCashError.setVisibility(View.VISIBLE);
                binding.textCashError.setText(message == null || message.trim().isEmpty()
                        ? "Kh\u00f4ng th\u1ec3 t\u1ea1o \u0111\u01a1n h\u00e0ng."
                        : message);
            }
        });
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
            return (int) Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    @Override
    public void onSessionChanged(User user) {
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        if (binding != null) {
            if (cartItems.isEmpty() && binding.layoutSuccess.getVisibility() != View.VISIBLE) {
                dismissAllowingStateLoss();
                return;
            }
            renderState();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        sessionManager.removeObserver(this);
        binding = null;
    }
}
